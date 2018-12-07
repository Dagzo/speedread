package net.dagzo.speedread;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "ResultSet", strict = false)
public class ResultSet {

    @Path("ma_result")
    @Element(name = "total_count")
    int total;

    @Path("ma_result/word_list")
    @ElementList(inline = true, required = false)
    List<Word> words = new ArrayList<>();

}
