package net.c0f3.tests.properymapper;


import net.c0f3.propmapper.core.MapperRegistry;
import net.c0f3.propmapper.core.PropertyMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Created by kostapc on 11.11.16.
 *
 */
public class TestFilesScan {

    @Test
    public void testFilesScan() throws IOException {

        PropertyMapper<SamplePojo> mapper = MapperRegistry.INSTANCE.getPropertyMapper(SamplePojo.class);
        Map<String, SamplePojo> objects = mapper.scan();

        Assert.assertEquals(3, objects.size());

        for (Map.Entry<String, SamplePojo> entry : objects.entrySet()) {
            String id = entry.getKey();
            SamplePojo pojo = entry.getValue();
            Assert.assertEquals(id,pojo.id);
            int value = pojo.someNumber;
            Assert.assertEquals("id_"+value,pojo.id);
            Assert.assertEquals("string_"+value,pojo.someString);
        }



    }

}
