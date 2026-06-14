package com.lab.ai.pusaman.service;

import com.google.common.collect.Sets;
import com.lab.ai.pusaman.entity.MarkdownFile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yang.nobel
 * @since 2026-06-13 18:19
 **/
@Service
public class MarkdownTitleExtractService {

    private static final Pattern PATTERN_TITLE = Pattern.compile("^#{1,6}\\s+(.*)");
    private static final Set<String> BIG_TITLE_SET = Sets.newHashSet("一、", "二、", "三、", "四、", "五、", "六、", "七、", "八、", "九、", "十、");

    public List<String> extractTitle(MarkdownFile file) {
        List<String> titles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getFile().getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PATTERN_TITLE.matcher(line.trim());
                if (matcher.find()) {
                    // 获取捕获组中的文本，即标题内容
                    String title = matcher.group(1);
                    if (BIG_TITLE_SET.stream().anyMatch(title::contains)) {
                        continue;
                    }
                    titles.add(title);
                }
            }
            return titles;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
