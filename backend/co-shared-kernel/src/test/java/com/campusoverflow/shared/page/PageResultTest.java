package com.campusoverflow.shared.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    void copiesItemsDefensively() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        PageResult<String> page = new PageResult<>(source, 2, 1, 10);
        source.add("c");

        assertThat(page.items()).containsExactly("a", "b");
        assertThatThrownBy(() -> page.items().add("d"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyHasNoItemsAndZeroTotal() {
        PageResult<String> page = PageResult.empty(2, 20);
        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void mapKeepsPagingMetadata() {
        PageResult<String> mapped = new PageResult<>(List.of(1, 2, 3), 7, 2, 3)
                .map(value -> "v" + value);

        assertThat(mapped.items()).containsExactly("v1", "v2", "v3");
        assertThat(mapped.total()).isEqualTo(7);
        assertThat(mapped.page()).isEqualTo(2);
        assertThat(mapped.size()).isEqualTo(3);
    }

    @Test
    void totalPagesRoundsUp() {
        assertThat(new PageResult<>(List.of(), 0, 1, 10).totalPages()).isZero();
        assertThat(new PageResult<>(List.of(), 1, 1, 10).totalPages()).isEqualTo(1);
        assertThat(new PageResult<>(List.of(), 10, 1, 10).totalPages()).isEqualTo(1);
        assertThat(new PageResult<>(List.of(), 11, 1, 10).totalPages()).isEqualTo(2);
    }

    @Test
    void totalPagesIsZeroWhenSizeIsZero() {
        assertThat(new PageResult<>(List.of(), 5, 1, 0).totalPages()).isZero();
    }
}
