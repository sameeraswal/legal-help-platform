package com.legalhelp.admin.repository;

import com.legalhelp.admin.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}
