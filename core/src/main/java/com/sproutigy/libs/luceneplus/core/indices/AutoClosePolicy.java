package com.sproutigy.libs.luceneplus.core.indices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
public class AutoClosePolicy {

    public static AutoClosePolicy DISABLED = AutoClosePolicy.builder().disable().build();
    public static AutoClosePolicy INSTANTLY = AutoClosePolicy.builder().instantly().build();
    public static AutoClosePolicy INSTANTLY_OPTIMIZE = AutoClosePolicy.builder().instantly().optimize().build();

    @Getter
    private boolean enabled = true;

    @Getter
    private long delayMillis;

    @Getter
    private boolean optimize;


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private long delayMillis = 0;
        private boolean optimize = false;

        public Builder disable() {
            enabled = false;
            return this;
        }

        public Builder instantly() {
            this.delayMillis = 0;
            return this;
        }

        public Builder delayMillis(long delayMillis) {
            this.delayMillis = delayMillis;
            return this;
        }

        public Builder delay(long delay, TimeUnit unit) {
            this.delayMillis = unit.toMillis(delay);
            return this;
        }

        public Builder optimize() {
            this.optimize = optimize;
            return this;
        }

        public AutoClosePolicy build() {
            return new AutoClosePolicy(enabled, delayMillis, optimize);
        }
    }
}
