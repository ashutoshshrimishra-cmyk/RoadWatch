package com.roadwatch.mobile.geocoding;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;

/**
 * Retrofit interface for OpenStreetMap Nominatim Geocoding API
 * FREE alternative to Google Geocoding API
 * 
 * Usage: https://nominatim.openstreetmap.org/search
 * Documentation: https://nominatim.org/release-docs/develop/api/Search/
 */
public interface NominatimService {

    /**
     * Forward geocoding: Address/place name to coordinates
     * 
     * @param query Search query (address, place name, etc.)
     * @param format Response format (json)
     * @param limit Maximum number of results
     * @param addressDetails Include detailed address breakdown
     * @param extraTags Include additional OpenStreetMap tags
     * @return List of geocoding results
     */
    @GET("search")
    Call<List<NominatimResult>> forwardGeocode(
            @Query("q") String query,
            @Query("format") String format, // "json"
            @Query("limit") int limit,
            @Query("addressdetails") int addressDetails, // 1 for true
            @Query("extratags") int extraTags, // 1 for true
            @Query("accept-language") String language // "en" for English
    );

    /**
     * Reverse geocoding: Coordinates to address
     * 
     * @param lat Latitude
     * @param lon Longitude
     * @param format Response format (json)
     * @param addressDetails Include detailed address breakdown
     * @param extraTags Include additional OpenStreetMap tags
     * @return Single geocoding result
     */
    @GET("reverse")
    Call<NominatimResult> reverseGeocode(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("format") String format, // "json"
            @Query("addressdetails") int addressDetails, // 1 for true
            @Query("extratags") int extraTags, // 1 for true
            @Query("accept-language") String language // "en" for English
    );
}