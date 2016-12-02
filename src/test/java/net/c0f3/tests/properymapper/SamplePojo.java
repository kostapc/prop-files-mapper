package net.c0f3.tests.properymapper;

import net.c0f3.propmapper.core.PropertyMap;
import net.c0f3.propmapper.core.PropertyMappedEntry;

/**
 * Created by kostapc on 11.11.16.
 *
 */
@PropertyMappedEntry(folder="test_folder")
public class SamplePojo {

    @PropertyMap(value="pojo.id", id=true)
    public String id;

    @PropertyMap("pojo.some.number")
    public Integer someNumber;

    @PropertyMap("pojo.some.string")
    public String someString;


}
