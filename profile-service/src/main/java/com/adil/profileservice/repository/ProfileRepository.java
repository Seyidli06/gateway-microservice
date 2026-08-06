package com.adil.profileservice.repository;

import com.adil.profileservice.model.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProfileRepository {

    private final Map<Long, Profile> profiles = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public Profile save(Profile profile) {
        if (profile.getId() == null) {
            profile.setId(idGenerator.incrementAndGet());
        }

        profiles.put(profile.getId(), profile);
        return profile;
    }

    public List<Profile> findAll() {
        return new ArrayList<>(profiles.values());
    }

    public Optional<Profile> findById(Long id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public boolean existsById(Long id) {
        return profiles.containsKey(id);
    }

    public void deleteById(Long id) {
        profiles.remove(id);
    }
}