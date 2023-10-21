package com.tomfran.lsm.io;

import com.tomfran.lsm.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;
import java.util.stream.Stream;

public class ExtendedStreamsTest {

    @TempDir
    static Path tempDirectory;

    static Random rn = new Random();

    @Test
    public void shouldReadWrite() {

        var os = new ExtendedOutputStream(tempDirectory + "stream");

        var intList = Stream.generate(rn::nextInt).map(Math::abs).limit(1000).toList();
        intList.forEach(os::writeVByteInt);

        var longList = Stream.generate(rn::nextLong).map(Math::abs).limit(1000).toList();
        longList.forEach(os::writeVByteLong);

        var pairList = Stream.generate(TestUtils::getRandomPair).limit(1000).toList();
        pairList.forEach(os::writeByteArrayPair);

        os.close();

        var is = new ExtendedInputStream(tempDirectory + "stream");

        intList.forEach(i -> {
            var read = is.readVByteInt();
            assert read == i;
        });

        longList.forEach(i -> {
            var read = is.readVByteLong();
            assert read == i;
        });

        pairList.forEach(i -> {
            var read = is.readBytePair();
            assert read != null;
            TestUtils.assertPairEqual(i, read);
        });
    }

}
