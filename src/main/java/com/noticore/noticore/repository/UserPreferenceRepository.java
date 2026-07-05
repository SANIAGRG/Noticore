package com.noticore.noticore.repository;

import com.noticore.noticore.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {
    // No custom methods needed yet -- findById(userId), save(), etc. from
    // JpaRepository already cover what we need until Phase 3.
}
