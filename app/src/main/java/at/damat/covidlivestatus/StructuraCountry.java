package at.damat.covidlivestatus;

import java.util.ArrayList;

public class StructuraCountry {
    public String country;
    public String isod2;

    public StructuraCountry(String country, String isod2) {
        this.country = country;
        this.isod2 = isod2;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIsod2() {
        return isod2;
    }

    public void setIsod2(String isod2) {
        this.isod2 = isod2;
    }

    public static class Pair <Boolean, Integer> {
        private final Boolean found;
        private final Integer position;

        public Pair(Boolean found, Integer position) {
            this.found = found;
            this.position = position;
        }

        public Boolean getFound() {
            return found;
        }

        public Integer getPosition() {
            return position;
        }
    }

    public static StructuraCountry.Pair<Boolean, Integer> GetPositionByCountry(ArrayList<StructuraCountry> structuraCountries, String country) {
        for (int i = 0; i < structuraCountries.size(); i++) {
            if (structuraCountries.get(i).country.equals(country)) {
                return new StructuraCountry.Pair<>(true, i);
            }
        }
        return new StructuraCountry.Pair<>(false, 0);
    }
}
