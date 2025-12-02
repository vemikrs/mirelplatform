/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import jp.vemi.mirel.foundation.abst.dao.repository.FeatureFlagRepository;
import jp.vemi.mirel.foundation.web.api.admin.dto.CreateFeatureFlagRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagListResponse;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateFeatureFlagRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagService単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagService単体テスト")
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    private FeatureFlagService featureFlagService;

    private FeatureFlag sampleFlag;

    @BeforeEach
    void setUp() {
        sampleFlag = new FeatureFlag();
        sampleFlag.setId("ff-test-001");
        sampleFlag.setFeatureKey("test.feature");
        sampleFlag.setFeatureName("テスト機能");
        sampleFlag.setDescription("テスト用の機能フラグ");
        sampleFlag.setApplicationId("promarker");
        sampleFlag.setStatus(FeatureStatus.STABLE);
        sampleFlag.setInDevelopment(false);
        sampleFlag.setRequiredLicenseTier(LicenseTier.FREE);
        sampleFlag.setEnabledByDefault(true);
        sampleFlag.setDeleteFlag(false);
    }

    @Nested
    @DisplayName("一覧取得テスト")
    class ListFeatureFlagsTest {

        @Test
        @DisplayName("正常系: フィーチャーフラグ一覧を取得できる")
        void shouldListFeatureFlags() {
            // Given
            List<FeatureFlag> flags = Arrays.asList(sampleFlag);
            Page<FeatureFlag> page = new PageImpl<>(flags);
            when(featureFlagRepository.findWithFilters(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

            // When
            FeatureFlagListResponse response = featureFlagService.listFeatureFlags(
                0, 10, null, null, null, null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getFeatures()).hasSize(1);
            assertThat(response.getFeatures().get(0).getFeatureKey()).isEqualTo("test.feature");
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("正常系: フィルタ条件で一覧を取得できる")
        void shouldListFeatureFlagsWithFilters() {
            // Given
            List<FeatureFlag> flags = Arrays.asList(sampleFlag);
            Page<FeatureFlag> page = new PageImpl<>(flags);
            when(featureFlagRepository.findWithFilters(
                eq("promarker"), eq(FeatureStatus.STABLE), eq(false), any(), any(Pageable.class)))
                .thenReturn(page);

            // When
            FeatureFlagListResponse response = featureFlagService.listFeatureFlags(
                0, 10, "promarker", FeatureStatus.STABLE, false, null);

            // Then
            assertThat(response.getFeatures()).hasSize(1);
            verify(featureFlagRepository).findWithFilters(
                eq("promarker"), eq(FeatureStatus.STABLE), eq(false), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("個別取得テスト")
    class GetFeatureFlagTest {

        @Test
        @DisplayName("正常系: IDでフィーチャーフラグを取得できる")
        void shouldGetFeatureFlagById() {
            // Given
            when(featureFlagRepository.findById("ff-test-001"))
                .thenReturn(Optional.of(sampleFlag));

            // When
            FeatureFlagDto result = featureFlagService.getFeatureFlagById("ff-test-001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("ff-test-001");
            assertThat(result.getFeatureKey()).isEqualTo("test.feature");
        }

        @Test
        @DisplayName("異常系: 存在しないIDで例外発生")
        void shouldThrowExceptionWhenNotFoundById() {
            // Given
            when(featureFlagRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                featureFlagService.getFeatureFlagById("non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Feature flag not found");
        }

        @Test
        @DisplayName("異常系: 論理削除済みフラグは取得できない")
        void shouldNotReturnDeletedFlag() {
            // Given
            sampleFlag.setDeleteFlag(true);
            when(featureFlagRepository.findById("ff-test-001"))
                .thenReturn(Optional.of(sampleFlag));

            // When & Then
            assertThatThrownBy(() -> 
                featureFlagService.getFeatureFlagById("ff-test-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Feature flag not found");
        }

        @Test
        @DisplayName("正常系: featureKeyでフィーチャーフラグを取得できる")
        void shouldGetFeatureFlagByKey() {
            // Given
            when(featureFlagRepository.findByFeatureKeyAndDeleteFlagFalse("test.feature"))
                .thenReturn(Optional.of(sampleFlag));

            // When
            FeatureFlagDto result = featureFlagService.getFeatureFlagByKey("test.feature");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFeatureKey()).isEqualTo("test.feature");
        }
    }

    @Nested
    @DisplayName("作成テスト")
    class CreateFeatureFlagTest {

        @Test
        @DisplayName("正常系: フィーチャーフラグを作成できる")
        void shouldCreateFeatureFlag() {
            // Given
            CreateFeatureFlagRequest request = new CreateFeatureFlagRequest();
            request.setFeatureKey("new.feature");
            request.setFeatureName("新機能");
            request.setDescription("新しい機能の説明");
            request.setApplicationId("promarker");
            request.setStatus(FeatureStatus.BETA);
            request.setInDevelopment(true);
            request.setRequiredLicenseTier(LicenseTier.PRO);

            when(featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse("new.feature"))
                .thenReturn(false);
            when(featureFlagRepository.save(any(FeatureFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeatureFlagDto result = featureFlagService.createFeatureFlag(request, "user-001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFeatureKey()).isEqualTo("new.feature");
            assertThat(result.getStatus()).isEqualTo(FeatureStatus.BETA);
            assertThat(result.getInDevelopment()).isTrue();

            ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
            verify(featureFlagRepository).save(captor.capture());
            FeatureFlag saved = captor.getValue();
            assertThat(saved.getCreateUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("異常系: 重複するfeatureKeyで例外発生")
        void shouldThrowExceptionWhenDuplicateKey() {
            // Given
            CreateFeatureFlagRequest request = new CreateFeatureFlagRequest();
            request.setFeatureKey("test.feature");

            when(featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse("test.feature"))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> 
                featureFlagService.createFeatureFlag(request, "user-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Feature key already exists");

            verify(featureFlagRepository, never()).save(any());
        }

        @Test
        @DisplayName("正常系: デフォルト値が設定される")
        void shouldSetDefaultValues() {
            // Given
            CreateFeatureFlagRequest request = new CreateFeatureFlagRequest();
            request.setFeatureKey("minimal.feature");
            request.setFeatureName("最小限の機能");
            request.setApplicationId("promarker");
            // status, inDevelopment, enabledByDefault, rolloutPercentageは未設定

            when(featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse("minimal.feature"))
                .thenReturn(false);
            when(featureFlagRepository.save(any(FeatureFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeatureFlagDto result = featureFlagService.createFeatureFlag(request, "user-001");

            // Then
            assertThat(result.getStatus()).isEqualTo(FeatureStatus.STABLE);
            assertThat(result.getInDevelopment()).isFalse();
            assertThat(result.getEnabledByDefault()).isTrue();
            assertThat(result.getRolloutPercentage()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("更新テスト")
    class UpdateFeatureFlagTest {

        @Test
        @DisplayName("正常系: フィーチャーフラグを更新できる")
        void shouldUpdateFeatureFlag() {
            // Given
            UpdateFeatureFlagRequest request = new UpdateFeatureFlagRequest();
            request.setFeatureName("更新された機能名");
            request.setStatus(FeatureStatus.DEPRECATED);

            when(featureFlagRepository.findById("ff-test-001"))
                .thenReturn(Optional.of(sampleFlag));
            when(featureFlagRepository.save(any(FeatureFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeatureFlagDto result = featureFlagService.updateFeatureFlag(
                "ff-test-001", request, "user-002");

            // Then
            assertThat(result.getFeatureName()).isEqualTo("更新された機能名");
            assertThat(result.getStatus()).isEqualTo(FeatureStatus.DEPRECATED);

            ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
            verify(featureFlagRepository).save(captor.capture());
            FeatureFlag saved = captor.getValue();
            assertThat(saved.getUpdateUserId()).isEqualTo("user-002");
        }

        @Test
        @DisplayName("正常系: 部分更新（未指定フィールドは変更しない）")
        void shouldPartiallyUpdateFeatureFlag() {
            // Given
            UpdateFeatureFlagRequest request = new UpdateFeatureFlagRequest();
            request.setInDevelopment(true); // これだけ変更

            when(featureFlagRepository.findById("ff-test-001"))
                .thenReturn(Optional.of(sampleFlag));
            when(featureFlagRepository.save(any(FeatureFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeatureFlagDto result = featureFlagService.updateFeatureFlag(
                "ff-test-001", request, "user-002");

            // Then
            assertThat(result.getFeatureName()).isEqualTo("テスト機能"); // 変更されていない
            assertThat(result.getStatus()).isEqualTo(FeatureStatus.STABLE); // 変更されていない
            assertThat(result.getInDevelopment()).isTrue(); // 変更された
        }
    }

    @Nested
    @DisplayName("削除テスト")
    class DeleteFeatureFlagTest {

        @Test
        @DisplayName("正常系: 論理削除できる")
        void shouldSoftDeleteFeatureFlag() {
            // Given
            when(featureFlagRepository.findById("ff-test-001"))
                .thenReturn(Optional.of(sampleFlag));
            when(featureFlagRepository.save(any(FeatureFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            featureFlagService.deleteFeatureFlag("ff-test-001", "user-003");

            // Then
            ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
            verify(featureFlagRepository).save(captor.capture());
            FeatureFlag saved = captor.getValue();
            assertThat(saved.getDeleteFlag()).isTrue();
            assertThat(saved.getUpdateUserId()).isEqualTo("user-003");
        }

        @Test
        @DisplayName("異常系: 存在しないフラグの削除で例外発生")
        void shouldThrowExceptionWhenDeleteNotFound() {
            // Given
            when(featureFlagRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                featureFlagService.deleteFeatureFlag("non-existent", "user-003"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Feature flag not found");
        }
    }

    @Nested
    @DisplayName("有効フィーチャー取得テスト")
    class GetAvailableFeaturesTest {

        @Test
        @DisplayName("正常系: 利用可能な機能を取得できる")
        void shouldGetAvailableFeatures() {
            // Given
            List<FeatureFlag> flags = Arrays.asList(sampleFlag);
            when(featureFlagRepository.findEffectiveFeatures("user-001", "tenant-001"))
                .thenReturn(flags);

            // When
            List<FeatureFlagDto> result = featureFlagService.getAvailableFeatures(
                "user-001", "tenant-001");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFeatureKey()).isEqualTo("test.feature");
        }

        @Test
        @DisplayName("正常系: 開発中の機能を取得できる")
        void shouldGetInDevelopmentFeatures() {
            // Given
            sampleFlag.setInDevelopment(true);
            List<FeatureFlag> flags = Arrays.asList(sampleFlag);
            when(featureFlagRepository.findByInDevelopmentTrueAndDeleteFlagFalse())
                .thenReturn(flags);

            // When
            List<FeatureFlagDto> result = featureFlagService.getInDevelopmentFeatures();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInDevelopment()).isTrue();
        }
    }

    @Nested
    @DisplayName("存在チェックテスト")
    class ExistsTest {

        @Test
        @DisplayName("正常系: 存在するfeatureKeyでtrueを返す")
        void shouldReturnTrueWhenExists() {
            // Given
            when(featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse("test.feature"))
                .thenReturn(true);

            // When
            boolean result = featureFlagService.existsByFeatureKey("test.feature");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("正常系: 存在しないfeatureKeyでfalseを返す")
        void shouldReturnFalseWhenNotExists() {
            // Given
            when(featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse("non-existent"))
                .thenReturn(false);

            // When
            boolean result = featureFlagService.existsByFeatureKey("non-existent");

            // Then
            assertThat(result).isFalse();
        }
    }
}
