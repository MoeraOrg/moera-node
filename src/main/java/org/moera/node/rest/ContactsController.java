package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.node.auth.Admin;
import org.moera.node.data.Contact;
import org.moera.node.data.QContact;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ContactInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/contacts")
@NoCache
public class ContactsController {

    public static final int MAX_CONTACTS_PER_REQUEST = 100;
    private static final int ARRANGEMENT_DEPTH = 5;

    @Inject
    private RequestContext requestContext;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @Admin
    @Transactional
    public List<ContactInfo> getAll(@RequestParam(defaultValue = "") String query,
                                    @RequestParam(required = false) Integer limit) {
        limit = limit != null && limit <= MAX_CONTACTS_PER_REQUEST ? limit : MAX_CONTACTS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }
        if (limit == 0) {
            return Collections.emptyList();
        }

        query = query.trim();
        String[] words = query.split("\\s+");

        QContact contact = QContact.contact;
        BooleanBuilder where = new BooleanBuilder();
        where.and(contact.nodeId.eq(requestContext.nodeId()));
        if (!ObjectUtils.isEmpty(query)) {
            for (String word : words) {
                String pattern = "%" + Util.le(word) + "%";
                where.andAnyOf(contact.remoteFullName.likeIgnoreCase(pattern),
                        contact.remoteNodeName.likeIgnoreCase(pattern));
            }
        }

        var request = new JPAQueryFactory(entityManager)
                .selectFrom(contact)
                .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
                .where(where)
                .orderBy(contact.closeness.desc())
                .limit(limit);

        List<Pattern> regexes = Arrays.stream(words)
                .map(word -> Pattern.compile("(?:^|\\s)" + Util.re(word), Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());

        int offset = 0;
        List<ContactInfo> result = new ArrayList<>();
        while (true) {
            List<Contact> page = request.offset(offset).fetch();
            if (page.isEmpty()) {
                return result;
            }
            page.stream()
                    .filter(ct -> contactMatch(ct, regexes))
                    .limit(limit - result.size())
                    .map(c -> new ContactInfo(c, requestContext.getOptions(), requestContext))
                    .forEach(result::add);
            if (result.size() >= limit) {
                return result;
            }
            offset += page.size();
        }
    }

    private boolean contactMatch(Contact contact, List<Pattern> regexes) {
        String haystack = !ObjectUtils.isEmpty(contact.getRemoteFullName())
                ? contact.getRemoteFullName() + " " + contact.getRemoteNodeName()
                : contact.getRemoteNodeName();
        List<Matcher> matchers = regexes.stream().map(regex -> regex.matcher(haystack)).collect(Collectors.toList());
        boolean allFound = matchers.stream().allMatch(Matcher::find);
        if (!allFound) {
            return false;
        }
        if (regexes.size() <= 1) {
            return true;
        }
        matchers.forEach(Matcher::reset);
        List<int[]> matches = matchers.stream()
                .map(m -> m.results().mapToInt(MatchResult::start).toArray())
                .collect(Collectors.toList());
        return hasArrangement(matches);
    }

    private boolean hasArrangement(List<int[]> values) {
        int size = Math.min(values.size(), ARRANGEMENT_DEPTH);
        int[] indexes = new int[size];
        Set<Integer> used = new HashSet<>();
        while (true) {
            used.clear();
            for (int i = 0; i < size; i++) {
                int value = values.get(i)[indexes[i]];
                if (used.contains(value)) {
                    break;
                }
                used.add(value);
            }
            if (used.size() == size) {
                return true;
            }
            for (int i = 0; i < size; i++) {
                indexes[i]++;
                if (indexes[i] < values.get(i).length) {
                    break;
                }
                if (i == size - 1) {
                    return false;
                }
                indexes[i] = 0;
            }
        }
    }

}
