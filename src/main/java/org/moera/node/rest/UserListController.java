package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.Scope;
import org.moera.node.data.UserListItem;
import org.moera.node.data.UserListItemRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.UserListItemAddedLiberin;
import org.moera.node.liberin.model.UserListItemDeletedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.UserListInfo;
import org.moera.node.model.UserListItemAttributes;
import org.moera.node.model.UserListItemInfo;
import org.moera.node.model.UserListSliceInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/user-lists")
@NoCache
public class UserListController {

    private static final Logger log = LoggerFactory.getLogger(UserListController.class);

    private static final int MAX_ITEMS_PER_REQUEST = 200;

    @Inject
    private RequestContext requestContext;

    @Inject
    private UserListItemRepository userListItemRepository;

    private final MomentFinder momentFinder = new MomentFinder();

    @GetMapping("/{name}")
    @Transactional
    public UserListInfo get(@PathVariable("name") String listName) {
        log.info("GET /user-lists/{name} (name = {})", LogUtil.format(listName));

        int total = userListItemRepository.countByList(requestContext.nodeId(), listName);

        return new UserListInfo(listName, total);
    }

    @GetMapping("/{name}/items")
    @Transactional
    public UserListSliceInfo getItems(
            @PathVariable("name") String listName,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /user-lists/{name}/items (name = {}, before = {}, after = {}, limit = {})",
                LogUtil.format(listName), LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        if (before != null && after != null) {
            throw new ValidationFailure("user-list-items.before-after-exclusive");
        }

        limit = limit != null && limit <= MAX_ITEMS_PER_REQUEST ? limit : MAX_ITEMS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        UserListSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getItemsBefore(listName, before, limit);
        } else {
            sliceInfo = getItemsAfter(listName, after, limit);
        }
        calcSliceTotals(sliceInfo);

        return sliceInfo;
    }

    private UserListSliceInfo getItemsBefore(String listName, long before, int limit) {
        UserListSliceInfo sliceInfo = new UserListSliceInfo(listName);
        sliceInfo.setBefore(before);
        Page<UserListItem> page = findSlice(requestContext.nodeId(), listName, SafeInteger.MIN_VALUE, before,
                limit + 1, Sort.Direction.DESC);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private UserListSliceInfo getItemsAfter(String listName, long after, int limit) {
        UserListSliceInfo sliceInfo = new UserListSliceInfo(listName);
        sliceInfo.setAfter(after);
        Page<UserListItem> page = findSlice(requestContext.nodeId(), listName, after, SafeInteger.MAX_VALUE,
                limit + 1, Sort.Direction.ASC);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private Page<UserListItem> findSlice(UUID nodeId, String listName, long afterMoment, long beforeMoment, int limit,
                                         Sort.Direction direction) {
        Pageable pageable = PageRequest.of(0, limit + 1, direction, "moment");
        return userListItemRepository.findInRange(nodeId, listName, afterMoment, beforeMoment, pageable);
    }

    private static void fillSlice(UserListSliceInfo sliceInfo, Page<UserListItem> page) {
        sliceInfo.setItems(page.getContent().stream().map(UserListItemInfo::new).collect(Collectors.toList()));
    }

    private void calcSliceTotals(UserListSliceInfo sliceInfo) {
        int total = userListItemRepository.countByList(requestContext.nodeId(), sliceInfo.getListName());
        sliceInfo.setTotal(total);
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(total - sliceInfo.getItems().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(total - sliceInfo.getItems().size());
        } else {
            int totalInFuture = userListItemRepository.countInRange(
                    requestContext.nodeId(), sliceInfo.getListName(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE);
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(total - totalInFuture - sliceInfo.getItems().size());
        }
    }

    @GetMapping("/{name}/items/{nodeName}")
    @Transactional
    public UserListItemInfo getItem(@PathVariable("name") String listName, @PathVariable String nodeName) {
        log.info("GET /user-lists/{name}/items/{nodeName} (name = {}, nodeName = {})",
                LogUtil.format(listName), LogUtil.format(nodeName));

        UserListItem item = userListItemRepository.findByListAndNodeName(requestContext.nodeId(), listName, nodeName)
                .orElseThrow(() -> new ObjectNotFoundFailure("user-list-item.not-found"));

        return new UserListItemInfo(item);
    }

    @PostMapping("/{name}/items")
    @Admin(Scope.USER_LISTS)
    @Transactional
    public ResponseEntity<UserListItemInfo> postItem(
            @PathVariable("name") String listName,
            @Valid @RequestBody UserListItemAttributes userListItemAttributes) {

        log.info("POST /user-lists/{name}/items (name = {}, nodeName = {})",
                LogUtil.format(listName), LogUtil.format(userListItemAttributes.getNodeName()));

        UserListItem item = userListItemRepository.findByListAndNodeName(
                requestContext.nodeId(), listName, userListItemAttributes.getNodeName()).orElse(null);
        if (item != null) {
            throw new OperationFailure("user-list-item.already-exists");
        }

        item = new UserListItem();
        item.setId(UUID.randomUUID());
        item.setNodeId(requestContext.nodeId());
        item.setListName(listName);
        item.setNodeName(userListItemAttributes.getNodeName());
        item.setMoment(momentFinder.find(
                moment -> userListItemRepository.countMoments(requestContext.nodeId(), moment) == 0,
                Util.now()));
        item = userListItemRepository.save(item);

        requestContext.send(new UserListItemAddedLiberin(item));

        return ResponseEntity
                .created(URI.create(String.format("/%s/items/%s", Util.ue(listName), Util.ue(item.getNodeName()))))
                .body(new UserListItemInfo(item));
    }

    @DeleteMapping("/{name}/items/{nodeName}")
    @Admin(Scope.USER_LISTS)
    @Transactional
    public Result deleteItem(@PathVariable("name") String listName, @PathVariable String nodeName) {
        log.info("DELETE /user-lists/{name}/items/{nodeName} (name = {}, nodeName = {})",
                LogUtil.format(listName), LogUtil.format(nodeName));

        UserListItem item = userListItemRepository.findByListAndNodeName(requestContext.nodeId(), listName, nodeName)
                .orElseThrow(() -> new ObjectNotFoundFailure("user-list-item.not-found"));
        userListItemRepository.delete(item);

        requestContext.send(new UserListItemDeletedLiberin(item));

        return Result.OK;
    }

}
