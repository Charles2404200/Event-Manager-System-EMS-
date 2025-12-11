package org.ems.application.service;

import org.ems.application.dto.UserDisplayRowDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for filtering user lists
 * Implements Single Responsibility Principle - only handles filtering logic
 * @author <your group number>
 */
public class UserFilteringService {

    /**
     * Filter users based on search term and role
     * @param users The list of users to filter
     * @param searchTerm The search term (empty string means no search filter)
     * @param roleFilter The role to filter by ("ALL" means no role filter)
     * @return Filtered list of users
     */
    public List<UserDisplayRowDTO> filterUsers(List<UserDisplayRowDTO> users, String searchTerm, String roleFilter) {
        long start = System.currentTimeMillis();
        System.out.println("🔎 [UserFilteringService] Filtering users - search: '" + searchTerm + "', role: " + roleFilter);

        if (users == null || users.isEmpty()) {
            System.out.println("  ℹ No users to filter");
            return new ArrayList<>();
        }

        List<UserDisplayRowDTO> filtered = new ArrayList<>();
        String lowerSearchTerm = searchTerm != null ? searchTerm.toLowerCase() : "";

        for (UserDisplayRowDTO user : users) {
            // Apply role filter
            if (!roleFilter.equals("ALL") && !user.getRole().equals(roleFilter)) {
                continue;
            }

            // Apply search filter
            if (lowerSearchTerm.isEmpty() ||
                user.getUsername().toLowerCase().contains(lowerSearchTerm) ||
                user.getFullName().toLowerCase().contains(lowerSearchTerm) ||
                user.getEmail().toLowerCase().contains(lowerSearchTerm)) {
                filtered.add(user);
            }
        }

        System.out.println("  ✓ Filtering completed in " + (System.currentTimeMillis() - start) + " ms: " +
                           filtered.size() + "/" + users.size());
        return filtered;
    }
}

