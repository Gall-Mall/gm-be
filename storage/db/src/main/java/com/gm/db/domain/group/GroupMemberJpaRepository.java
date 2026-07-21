package com.gm.db.domain.group;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberJpaRepository extends JpaRepository<GroupMemberEntity, UUID> {
}
