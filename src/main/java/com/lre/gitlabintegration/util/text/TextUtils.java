package com.lre.gitlabintegration.util.text;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

@UtilityClass
public class TextUtils {


    public static String toTitleCase(String input) {
        if (StringUtils.isBlank(input)) return "";
        return WordUtils.capitalizeFully(StringUtils.normalizeSpace(input));
    }

}
