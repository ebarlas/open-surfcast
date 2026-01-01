package org.opensurfcast.http;

import java.io.IOException;
import java.io.InputStream;

public interface Parser<T> {

    T parse(InputStream inputStream) throws IOException;

}
