package enums;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class CropStateTest {
    @Test
    public void getByData() {
        for (CropState cropState : CropState.values()) {
            assertThat(CropState.getByData(cropState.getData()), is(cropState));
        }
    }
}
