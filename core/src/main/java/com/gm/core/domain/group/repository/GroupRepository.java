package com.gm.core.domain.group.repository;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;

public interface GroupRepository {

    /**
     * 그룹을 저장하고 요청 회원을 OWNER 역할·ACTIVE 상태의 멤버로 함께 등록한다.
     *
     * <p>그룹과 그룹장 멤버 등록은 하나의 원자적 작업으로 처리한다.</p>
     *
     * @param newGroup 그룹 생성 명세
     * @return 저장된 그룹
     */
    Group create(NewGroup newGroup);
}
