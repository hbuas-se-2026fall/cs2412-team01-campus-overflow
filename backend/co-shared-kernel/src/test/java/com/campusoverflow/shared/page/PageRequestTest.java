package com.campusoverflow.shared.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageRequestTest {

    @Test
    void clampsPageToAtLeastOne() {
        assertThat(new PageRequest(0, 10).page()).isEqualTo(1);
        assertThat(new PageRequest(-5, 10).page()).isEqualTo(1);
    }

    @Test
    void clampsSizeIntoOneToFifty() {
        assertThat(new PageRequest(1, 0).size()).isEqualTo(1);
        assertThat(new PageRequest(1, -3).size()).isEqualTo(1);
        assertThat(new PageRequest(1, 51).size()).isEqualTo(PageRequest.MAX_SIZE);
        assertThat(new PageRequest(1, 500).size()).isEqualTo(50);
    }

    @Test
    void keepsValidValuesUntouched() {
        PageRequest request = new PageRequest(3, 20);
        assertThat(request.page()).isEqualTo(3);
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void computesOffsetFromClampedValues() {
        assertThat(new PageRequest(1, 20).offset()).isZero();
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(40);
        // 越界参数先被夹紧，再用夹紧后的值算偏移。
        assertThat(new PageRequest(0, 999).offset()).isZero();
        assertThat(new PageRequest(2, 999).offset()).isEqualTo(50);
    }
}
