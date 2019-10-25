package se.kth.jdbl.wrapper;

import java.util.Arrays;

public enum DebloatTypeEnum {

    TEST_DEBLOAT,
    ENTRY_POINT_DEBLOAT,
    CONSERVATIVE_DEBLOAT;

    @Override
    public String toString() {
        return "Debloat methods: " + Arrays.toString(DebloatTypeEnum.values());
    }
}
