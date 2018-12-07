package net.dagzo.speedread;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "word", strict = false)
public class Word {

    @Element
    String surface;

    @Element
    String reading;

    @Element
    String pos;
}
