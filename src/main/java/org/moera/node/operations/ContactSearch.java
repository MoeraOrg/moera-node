package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.node.data.Contact;
import org.moera.node.data.QContact;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ContactSearch {

    public static final int MAX_CONTACTS_PER_REQUEST = 100;
    private static final int ARRANGEMENT_DEPTH = 5;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Contact> search(UUID nodeId, String query, int limit) {
        return search(nodeId, query, limit, null);
    }

    public List<Contact> search(UUID nodeId, String query, int limit, Predicate extraWhere) {
        query = query != null ? query.trim() : "";
        String[] words = ObjectUtils.isEmpty(query) ? new String[0] : query.split("\\s+");

        QContact contact = QContact.contact;
        BooleanBuilder where = new BooleanBuilder();
        where.and(contact.nodeId.eq(nodeId));
        if (extraWhere != null) {
            where.and(extraWhere);
        }
        if (!ObjectUtils.isEmpty(query)) {
            for (String word : words) {
                String pattern = "%" + Util.le(word) + "%";
                where.andAnyOf(
                    contact.remoteFullName.likeIgnoreCase(pattern),
                    contact.remoteNodeName.likeIgnoreCase(pattern)
                );
            }
        }

        var request = new JPAQueryFactory(entityManager)
            .selectFrom(contact)
            .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
            .where(where)
            .orderBy(contact.distance.asc())
            .limit(limit);

        List<Pattern> regexes = Arrays.stream(words)
            .map(word -> Pattern.compile("(?:^|\\s)" + Util.re(word), Pattern.CASE_INSENSITIVE))
            .collect(Collectors.toList());

        int offset = 0;
        List<Contact> result = new ArrayList<>();
        while (true) {
            List<Contact> page = request.offset(offset).fetch();
            if (page.isEmpty()) {
                return result;
            }
            page.stream()
                .filter(ct -> contactMatch(ct, regexes))
                .limit(limit - result.size())
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
        List<Matcher> matchers = regexes.stream().map(regex -> regex.matcher(haystack)).toList();
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
