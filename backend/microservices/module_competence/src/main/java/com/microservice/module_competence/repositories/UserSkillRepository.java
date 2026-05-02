package com.microservice.module_competence.repositories;

import com.microservice.module_competence.entities.UserSkill;
import com.microservice.module_competence.enums.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserId(String userId);
    List<UserSkill> findByUserIdAndLevel(String userId, Level level);
    Optional<UserSkill> findByUserIdAndSkillId(String userId, Long skillId);
    boolean existsByUserIdAndSkillId(String userId, Long skillId);
    List<UserSkill> findBySkillId(Long skillId);
    @Query("SELECT us.skill.id, us.skill.name, COUNT(us) as freelancerCount FROM UserSkill us GROUP BY us.skill.id, us.skill.name ORDER BY freelancerCount DESC")
    List<Object[]> countFreelancersBySkill();

    @Query("SELECT us.level, COUNT(us) as count FROM UserSkill us WHERE us.skill.id = :skillId GROUP BY us.level")
    List<Object[]> countLevelsBySkill(@Param("skillId") Long skillId);

    @Query("SELECT us.level, COUNT(us) as count FROM UserSkill us GROUP BY us.level")
    List<Object[]> countAllLevels();
}