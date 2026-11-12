package com.campusoverflow.qa.infrastructure.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommonmarkContentRendererTest {

    private final CommonmarkContentRenderer renderer = new CommonmarkContentRenderer();

    // ---------- toSafeHtml ----------

    @Test
    void rendersBasicMarkdown() {
        assertThat(renderer.toSafeHtml("**加粗** 与 *斜体*"))
                .contains("<strong>加粗</strong>")
                .contains("<em>斜体</em>");
    }

    @Test
    void rendersFencedCodeBlockWithLanguageClass() {
        String html = renderer.toSafeHtml("```java\nint a = 1;\n```");

        assertThat(html).contains("<pre>").contains("<code");
        assertThat(html).contains("language-java");
        // 清洗器会把等号等符号转成实体（&#61;），浏览器渲染结果仍是 "="，此处只断言代码内容本身存在。
        assertThat(html).contains("int a").contains("1;");
    }

    @Test
    void rendersTables() {
        String html = renderer.toSafeHtml("| a | b |\n| - | - |\n| 1 | 2 |");

        assertThat(html).contains("<table>");
        assertThat(html).contains("<th>a</th>");
        assertThat(html).contains("<td>1</td>");
    }

    @Test
    void stripsRawScriptTag() {
        String html = renderer.toSafeHtml("正常文本<script>alert('xss')</script>");

        // 第一道防线（commonmark escapeHtml）已转义，第二道防线（Sanitizer）兜底移除。
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("</script>");
        assertThat(html).contains("正常文本");
    }

    @Test
    void stripsDangerousUrlSchemes() {
        String html = renderer.toSafeHtml("[点我](javascript:alert('xss'))");

        assertThat(html.toLowerCase()).doesNotContain("javascript:");
    }

    @Test
    void keepsOrdinaryLinks() {
        String html = renderer.toSafeHtml("[校园论坛](https://example.edu/posts)");

        assertThat(html).contains("href=\"https://example.edu/posts\"");
    }

    @Test
    void blankInputReturnsEmptyString() {
        assertThat(renderer.toSafeHtml(null)).isEmpty();
        assertThat(renderer.toSafeHtml("   ")).isEmpty();
    }

    // ---------- toPlainText ----------

    @Test
    void plainTextDropsMarkupAndCollapsesWhitespace() {
        String text = renderer.toPlainText("# 标题\n\n正文 **重点**\n\n\n第二段", 200);

        assertThat(text).isEqualTo("标题 正文 重点 第二段");
        assertThat(text).doesNotContain("#").doesNotContain("**");
    }

    @Test
    void plainTextTruncatesByCodePoint() {
        String text = renderer.toPlainText("abcdefghij", 4);

        assertThat(text).isEqualTo("abcd…");
    }

    @Test
    void plainTextKeepsShortTextUnchanged() {
        assertThat(renderer.toPlainText("短文本", 10)).isEqualTo("短文本");
    }

    @Test
    void plainTextHandlesNull() {
        assertThat(renderer.toPlainText(null, 10)).isEmpty();
    }
}
