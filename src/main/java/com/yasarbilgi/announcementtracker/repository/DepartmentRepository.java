package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Department> findByName(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    @Override
    @EntityGraph(attributePaths = {"sites", "subscribers"})
    Optional<Department> findById(Long id);

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.sites LEFT JOIN FETCH d.subscribers")
    List<Department> findAllWithDetails();
}
