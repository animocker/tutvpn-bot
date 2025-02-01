package com.tutvpn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("select u.id from UserEntity u where u.expireDate >= :date")
    List<Long> getActiveUsers(LocalDate date);
}
