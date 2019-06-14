package org.moera.node.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicPageRepository extends JpaRepository<PublicPage, Long> {

    PublicPage findByBeginMoment(long beginMoment);

    PublicPage findByEndMoment(long endMoment);

    @Query("select p from PublicPage p where ?1 >= p.beginMoment and ?1 < p.endMoment")
    PublicPage findContaining(long moment);

}
