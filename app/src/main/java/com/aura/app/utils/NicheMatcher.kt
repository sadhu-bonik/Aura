package com.aura.app.utils

/**
 * NicheMatcher — pure utility for niche tag comparison.
 *
 * All comparison is done on normalized tags (trimmed + lowercase) so that
 * "Fitness ", "fitness", and "FITNESS" all count as the same tag.
 */
object NicheMatcher {

    /** Normalize a single tag: trim whitespace and lowercase. */
    fun normalizeTag(tag: String): String = tag.trim().lowercase()

    /** Normalize an entire list of tags. */
    fun normalizeTags(tags: List<String>): Set<String> =
        tags.map { normalizeTag(it) }.filter { it.isNotBlank() }.toSet()

    /**
     * Compute the number of shared tags between two creators.
     *
     * @param viewerTags   Normalized tags of the logged-in creator (the viewer).
     * @param candidateTags Normalized tags of the candidate creator being evaluated.
     * @return Number of overlapping tags (0 if either list is empty).
     */
    fun overlapCount(viewerTags: Set<String>, candidateTags: Set<String>): Int {
        if (viewerTags.isEmpty() || candidateTags.isEmpty()) return 0
        return viewerTags.intersect(candidateTags).size
    }
}
