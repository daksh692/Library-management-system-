package com.library.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Every tunable library policy, bound from the {@code app.*} namespace.
 *
 * <p>Rules.md #1 and #6: thresholds are configuration, never literals in code.</p>
 */
@Component
@ConfigurationProperties(prefix = "app")
public class LibraryProperties {

    private final Loan loan = new Loan();
    private final Penalty penalty = new Penalty();
    private final Hold hold = new Hold();
    private final Card card = new Card();
    private final RateLimit rateLimit = new RateLimit();

    public Loan getLoan() { return loan; }
    public Penalty getPenalty() { return penalty; }
    public Hold getHold() { return hold; }
    public Card getCard() { return card; }
    public RateLimit getRateLimit() { return rateLimit; }

    /** Borrowing policy. */
    public static class Loan {
        /** Days a book may be kept before it is overdue. */
        private int periodDays = 14;
        /** Maximum concurrently issued or reserved books per patron. */
        private int maxActiveBooks = 5;

        public int getPeriodDays() { return periodDays; }
        public void setPeriodDays(int v) { this.periodDays = v; }
        public int getMaxActiveBooks() { return maxActiveBooks; }
        public void setMaxActiveBooks(int v) { this.maxActiveBooks = v; }
    }

    /** Fine policy. */
    public static class Penalty {
        /** Charged per day past the due date. */
        private double perDayLate = 1.0;
        /** Fraction of the book price charged when returned damaged. */
        private double damagedRate = 0.5;
        /** Fraction of the book price charged when reported lost. */
        private double lostRate = 1.0;
        /** Used when a book record carries no price. */
        private double defaultBookPrice = 50.0;

        public double getPerDayLate() { return perDayLate; }
        public void setPerDayLate(double v) { this.perDayLate = v; }
        public double getDamagedRate() { return damagedRate; }
        public void setDamagedRate(double v) { this.damagedRate = v; }
        public double getLostRate() { return lostRate; }
        public void setLostRate(double v) { this.lostRate = v; }
        public double getDefaultBookPrice() { return defaultBookPrice; }
        public void setDefaultBookPrice(double v) { this.defaultBookPrice = v; }
    }

    /** Reservation pickup policy. */
    public static class Hold {
        /** Hours a reserved book is held before the hold lapses. */
        private int windowHours = 48;
        /** How often the expiry sweep runs, in milliseconds. */
        private long sweepIntervalMs = 3_600_000L;

        public int getWindowHours() { return windowHours; }
        public void setWindowHours(int v) { this.windowHours = v; }
        public long getSweepIntervalMs() { return sweepIntervalMs; }
        public void setSweepIntervalMs(long v) { this.sweepIntervalMs = v; }
    }

    /** Library card policy. */
    public static class Card {
        /** Months a new card stays valid. */
        private int validityMonths = 12;
        /** When true, an expired card blocks borrowing. */
        private boolean enforceExpiry = true;

        public int getValidityMonths() { return validityMonths; }
        public void setValidityMonths(int v) { this.validityMonths = v; }
        public boolean isEnforceExpiry() { return enforceExpiry; }
        public void setEnforceExpiry(boolean v) { this.enforceExpiry = v; }
    }

    /** Rate limiting policy. */
    public static class RateLimit {
        private final Tier auth = new Tier(5, 1);
        private final Tier publicApi = new Tier(30, 1);
        private final Tier authenticated = new Tier(100, 1);

        public Tier getAuth() { return auth; }
        public Tier getPublicApi() { return publicApi; }
        public Tier getAuthenticated() { return authenticated; }

        public static class Tier {
            private int capacity;
            private int refillMinutes;

            public Tier() {}
            public Tier(int capacity, int refillMinutes) {
                this.capacity = capacity;
                this.refillMinutes = refillMinutes;
            }

            public int getCapacity() { return capacity; }
            public void setCapacity(int capacity) { this.capacity = capacity; }
            public int getRefillMinutes() { return refillMinutes; }
            public void setRefillMinutes(int refillMinutes) { this.refillMinutes = refillMinutes; }
        }
    }
}
