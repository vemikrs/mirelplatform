package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dao.repository.MsteStencilRepository;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(classes = jp.vemi.mirel.MiplaApplication.class)
@ActiveProfiles("e2e")
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(properties = {
        "spring.session.jdbc.initialize-schema=never",
        "spring.sql.init.mode=never"
})
public class SuggestServiceImpTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Autowired
    SuggestServiceImp service;

    @MockBean
    MsteStencilRepository repository;

    @MockBean
    ResourcePatternResolver resolver; // engine内部呼び出しの null 回避

    private MsteStencil make(String cd, String name, String kind) {
        MsteStencil s = new MsteStencil();
        s.stencilCd = cd;
        s.stencilName = name;
        s.itemKind = kind; // 0:category,1:item
        return s;
    }

    @BeforeEach
    void setup() {
        // デフォルト: categories + items for /samples
        List<MsteStencil> all = Arrays.asList(
                make("/samples", "Sample Stencils", "0"),
                make("/samples/hello-world", "Hello World Generator", "1"));
        Mockito.when(repository.findAll()).thenReturn(all);
        Mockito.when(repository.findByStencilCd(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String cd = inv.getArgument(0);
                    String kind = inv.getArgument(1);
                    if ("/samples".equals(cd) && "1".equals(kind)) {
                        return Collections.singletonList(make("/samples/hello-world", "Hello World Generator", "1"));
                    }
                    return Collections.emptyList();
                });
    }

    private ApiResponse<SuggestResult> invoke(SuggestParameter p) {
        return service.invoke(ApiRequest.<SuggestParameter>builder().model(p).build());
    }

    @Test
    @DisplayName("初期ロード: selectFirstIfWildcard=false で全未選択")
    void initialLoad() {
        ApiResponse<SuggestResult> resp = invoke(SuggestParameter.builder()
                .stencilCategory("*")
                .stencilCd("*")
                .serialNo("*")
                .selectFirstIfWildcard(false)
                .build());
        SuggestResult model = extract(resp);
        assertNotNull(model.fltStrStencilCategory);
        assertTrue(model.fltStrStencilCategory.items.size() > 0, "Category list should not be empty");
        assertEquals("", model.fltStrStencilCategory.selected, "Category should not be auto-selected");
        // 早期 return: category未確定のためstencil以降は初期化されたまま（空リスト）
        assertNotNull(model.fltStrStencilCd, "Stencil field should be initialized");
        assertTrue(model.fltStrStencilCd.items.isEmpty(), "Stencil items should be empty when category not selected");
        assertNull(model.fltStrStencilCd.selected, "Stencil selected should be null");
    }

    @Test
    @DisplayName("カテゴリ auto-select → ステンシル/serial cascade")
    void categoryAutoSelect() {
        ApiResponse<SuggestResult> resp = invoke(SuggestParameter.builder()
                .stencilCategory("*")
                .stencilCd("*")
                .serialNo("*")
                .selectFirstIfWildcard(true)
                .build());
        SuggestResult model = extract(resp);
        assertNotNull(model.fltStrStencilCategory);
        assertEquals("/samples", model.fltStrStencilCategory.selected);
        // stencil list 取得済み
        assertNotNull(model.fltStrStencilCd);
        assertEquals("/samples/hello-world", model.fltStrStencilCd.selected);
        // serial は auto select され engine が呼ばれている想定 (serial list が空なら selected 空)
        assertNotNull(model.fltStrSerialNo);
    }

    @Nested
    class SerialPhase {
        @Test
        @DisplayName("serial=* かつ selectFirstIfWildcard=false で params 未取得")
        void serialPending() {
            ApiResponse<SuggestResult> resp = invoke(SuggestParameter.builder()
                    .stencilCategory("/samples")
                    .stencilCd("/samples/hello-world")
                    .serialNo("*")
                    .selectFirstIfWildcard(false)
                    .build());
            SuggestResult model = extract(resp);
            assertNotNull(model.fltStrSerialNo);
            assertEquals("", model.fltStrSerialNo.selected);
            // serial 未確定なので params/stencil は null/未設定
            assertTrue(model.params == null || model.params.childs == null || model.params.childs.isEmpty());
        }
    }

    private SuggestResult extract(ApiResponse<SuggestResult> resp) {
        // ModelWrapper(data.model) の取り出し (unsafe cast 前提)
        Object data = resp.getData();
        try {
            java.lang.reflect.Field f = data.getClass().getField("model");
            return (SuggestResult) f.get(data);
        } catch (Exception e) {
            fail("ModelWrapper unwrap failed: " + e.getMessage());
            return null;
        }
    }
}
