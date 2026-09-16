package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Department> findByName(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.sites")
    List<Department> findAllWithSites();
}
