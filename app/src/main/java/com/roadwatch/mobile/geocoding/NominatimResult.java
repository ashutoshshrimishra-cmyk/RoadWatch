package com.roadwatch.mobile.geocoding;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Data model for OpenStreetMap Nominatim API responses
 * FREE alternative to Google Geocoding API responses
 */
public class NominatimResult {

    @SerializedName("place_id")
    public long placeId;

    @SerializedName("licence")
    public String licence;

    @SerializedName("osm_type")
    public String osmType;

    @SerializedName("osm_id")
    public long osmId;

    @SerializedName("lat")
    public String latitude;

    @SerializedName("lon")
    public String longitude;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("class")
    public String placeClass;

    @SerializedName("type")
    public String placeType;

    @SerializedName("importance")
    public Double importance;

    @SerializedName("icon")
    public String icon;

    @SerializedName("address")
    public AddressDetails address;

    @SerializedName("extratags")
    public Map<String, String> extraTags;

    @SerializedName("boundingbox")
    public String[] boundingBox;

    /**
     * Detailed address breakdown
     */
    public static class AddressDetails {
        @SerializedName("house_number")
        public String houseNumber;

        @SerializedName("road")
        public String road;

        @SerializedName("suburb")
        public String suburb;

        @SerializedName("neighbourhood")
        public String neighbourhood;

        @SerializedName("city")
        public String city;

        @SerializedName("town")
        public String town;

        @SerializedName("village")
        public String village;

        @SerializedName("municipality")
        public String municipality;

        @SerializedName("county")
        public String county;

        @SerializedName("state")
        public String state;

        @SerializedName("postcode")
        public String postcode;

        @SerializedName("country")
        public String country;

        @SerializedName("country_code")
        public String countryCode;

        /**
         * Get the best available city name from various fields
         */
        public String getBestCityName() {
            if (city != null && !city.isEmpty()) return city;
            if (town != null && !town.isEmpty()) return town;
            if (village != null && !village.isEmpty()) return village;
            if (municipality != null && !municipality.isEmpty()) return municipality;
            if (county != null && !county.isEmpty()) return county;
            return "Unknown City";
        }

        /**
         * Get a formatted address string
         */
        public String getFormattedAddress() {
            StringBuilder sb = new StringBuilder();
            
            if (houseNumber != null && road != null) {
                sb.append(houseNumber).append(" ").append(road);
            } else if (road != null) {
                sb.append(road);
            }
            
            String cityName = getBestCityName();
            if (!cityName.equals("Unknown City")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(cityName);
            }
            
            if (state != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(state);
            }
            
            if (postcode != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(postcode);
            }
            
            return sb.length() > 0 ? sb.toString() : "Unknown Address";
        }
    }

    /**
     * Get latitude as double
     */
    public double getLatitudeAsDouble() {
        try {
            return Double.parseDouble(latitude);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Get longitude as double
     */
    public double getLongitudeAsDouble() {
        try {
            return Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Get a user-friendly address string
     */
    public String getFriendlyAddress() {
        if (address != null) {
            return address.getFormattedAddress();
        }
        return displayName != null ? displayName : "Unknown Location";
    }
}