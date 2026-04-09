---
name: Backend Development Patterns
description: mirelplatform バックエンド拡張のためのJavaコードパターン集。新規API追加、テンプレートエンジン統合、DB拡張の手順を提供する。
---

# Backend Development Patterns

mirelplatform (Spring Boot 3.3 + Java 21) にカスタム機能を追加する際のパターン集。

## レイヤードアーキテクチャ

```
jp.vemi.mirel.apps.mste/
├── application/
│   └── controller/  ApiController (@RequestMapping("apps/mste/api"))
├── domain/
│   ├── dto/         リクエスト/レスポンス DTO
│   ├── service/     ビジネスロジック
│   └── entity/      JPA エンティティ
└── infrastructure/
    └── repository/  Spring Data JPA
```

Controller は `{path}Api` 命名で Bean を自動解決するため、API 追加時に Controller の変更は不要。

---

## 新規 API 追加手順

### Step 1: DTO 定義 (`domain/dto`)

```java
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CustomParameter {
    private String parameterName;
    private String parameterValue;
}

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CustomResult {
    private String resultData;
    private List<String> generatedFiles;
}
```

### Step 2: Service 実装 (`domain/service`)

```java
@Service
@Transactional
public class CustomServiceImp implements CustomService {

    @Override
    public ApiResponse<CustomResult> invoke(ApiRequest<CustomParameter> parameter) {
        ApiResponse<CustomResult> response = ApiResponse.<CustomResult>builder().build();
        try {
            CustomResult result = processLogic(parameter.getModel());
            response.setData(result);
        } catch (Exception e) {
            response.addError("Processing failed: " + e.getMessage());
            logger.error("Custom service error", e);
        }
        return response;
    }
}
```

### Step 3: API Facade (`application/api`)

```java
@Service  // Bean名: "customApi" → Controller が /api/custom で自動解決
public class CustomApi implements MsteApi {

    @Autowired
    protected CustomService service;

    @Override
    public ApiResponse<?> service(Map<String, Object> request) {
        Map<String, Object> content = InstanceUtil.forceCast(request.get("content"));
        CustomParameter param = CustomParameter.builder()
            .parameterName((String) content.get("parameterName"))
            .build();
        return service.invoke(
            ApiRequest.<CustomParameter>builder().model(param).build()
        );
    }
}
```

> 命名規則: `{FunctionName}Api` が `MsteApi` を実装 → Spring が `{functionName}Api` で Bean 登録 → Controller が自動解決。

---

## テンプレートエンジン統合 (FreeMarker)

### カスタム Function Resolver

```java
@Component
public class CustomFunctionResolver extends FunctionResolverAbstract {

    @Override
    public boolean is(FunctionResolverCondition condition) {
        return "customFunction".equals(condition.getFunctionName());
    }

    @Override
    public String resolve(FunctionResolverCondition condition) {
        List<Object> args = condition.getArgs();
        if (args.size() < 2) {
            throw new IllegalArgumentException("customFunction requires 2 arguments");
        }
        String input = args.get(0).toString();
        String format = args.get(1).toString();
        return input.toUpperCase() + "_" + format;
    }
}
```

### FreeMarker テンプレートでの利用

```freemarker
package ${packageName};

public class ${className} {
    <#list parameters as param>
    private ${param.type} ${param.name};
    </#list>
}
```

---

## DB エンティティ / リポジトリ追加

```java
@Entity
@Table(name = "CUSTOM_TEMPLATE")
@lombok.Data @lombok.Builder
@lombok.NoArgsConstructor @lombok.AllArgsConstructor
public class CustomTemplate {
    @Id
    @Column(name = "template_id", length = 100)
    private String templateId;

    @Column(name = "template_name", length = 200)
    private String templateName;

    @Lob
    @Column(name = "template_content")
    private String templateContent;
}

@Repository
public interface CustomTemplateRepository extends JpaRepository<CustomTemplate, String> {
    List<CustomTemplate> findByTemplateCategory(String category);

    @Query("SELECT c FROM CustomTemplate c WHERE c.templateName LIKE %:pattern%")
    List<CustomTemplate> findByNamePattern(@Param("pattern") String pattern);
}
```

> Flyway 未導入のため、スキーマ変更は `docs/` にSQL差分を記録すること。
