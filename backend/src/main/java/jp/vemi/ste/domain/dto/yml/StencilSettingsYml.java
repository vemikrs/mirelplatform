/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.ste.domain.dto.yml;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * StencilSettingsYml
 */
// Yaml用のStencil設定モデルでうす。
@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = false)
public class StencilSettingsYml {

  protected Stencil stencil;

  /**
   * Stencil settings.
   */
  @Setter
  @Getter
  @NoArgsConstructor
  @Accessors(chain = false)
  public static class Stencil {
    protected Config config;
    protected List<Map<String, Object>> dataElement;
    protected List<Map<String, Object>> dataDomain;
    protected CodeInfo codeInfo;
    protected List<Store> store;

    /**
     * Configurations.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Config {
      protected String id;
      protected String name;
      protected String categoryId;
      protected String categoryName;
      protected String serial;
      protected String lastUpdate;
      protected String lastUpdateUser;
      protected String description;
    }

    /**
     * Doc meta.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class CodeInfo {
      protected String copyright;
      protected String versionNo = "1.0";
      protected String author;
      protected String vendor;
    }
  }

  /**
   * Store
   */
  @Setter
  @Getter
  @NoArgsConstructor
  public static class Store {
    protected String id;
    protected List<ValueText> items;
    @Setter
    @Getter
    @NoArgsConstructor
      public static class ValueText {
      protected String value;
      protected String text;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  /**
   * getStencilDeAndDd.<br/>
   * 
   * @return list
   */
  public List<Map<String, Object>> getStencilDeAndDd() {
    return mergeStencilDeAndDd(this.getStencil().getDataElement(), this.getStencil().getDataDomain());
  }

  /**
   * appendDataElementSublist
   * @param dataDomains 追加するデータドメイン
   */
  public void appendDataElementSublist(List<Map<String, Object>> dataDomains) {
    this.stencil.dataDomain = mergeMapList(this.stencil.dataDomain, dataDomains);
  }

  /**
   * mergeStencilDeAndDd.<br/>
   * 
   * @param dataElements
   * @param dataDomains
   * @return
   */
  protected static List<Map<String, Object>> mergeStencilDeAndDd(List<Map<String, Object>> dataElements,
      List<Map<String, Object>> dataDomains) {
    return mergeMapList(dataElements, dataDomains);
  }

  /**
   * mergeMapList.<br/>
   * 
   * @param list1pm
   * @param list2pm
   * @return
   */
  @SuppressWarnings("null")
protected static List<Map<String, Object>> mergeMapList(List<Map<String, Object>> list1pm,
      List<Map<String, Object>> list2pm) {

    if(null == list1pm) {
      list1pm = Lists.newArrayList();
    }
    if(null == list2pm) {
      list2pm = Lists.newArrayList();
    }
    // create list1map
    final Map<String, Map<String, Object>> list1map = Maps.newLinkedHashMap();
    for (Map<String, Object> list1item : list1pm) {
      if (list1item.containsKey("id")) {
        list1map.put((String)list1item.get("id"), list1item);
      }
    }

    // create list2map
    final Map<String, Map<String, Object>> list2map = Maps.newLinkedHashMap();
    for (Map<String, Object> list2item : list2pm) {
      if (list2item.containsKey("id")) {
        list2map.put((String)list2item.get("id"), list2item);
      }
    }

    final Map<String, Map<String, Object>> mergedItems = Maps.<String, Map<String, Object>>newLinkedHashMap();
    mergedItems.putAll(list1map);

    for (Map<String, Object> list1item : list1pm) {

      Map<String, Object> mergedMap = list1item != null ? Maps.newLinkedHashMap(list1item) : Maps.newLinkedHashMap();
      final String id = (String) mergedMap.get("id");
      if (list2map.containsKey(id)) {
        list2map.get(id).entrySet().forEach(entry -> {
          if(false == mergedMap.containsKey(entry.getKey())) {
            mergedMap.put(entry.getKey(), entry.getValue());
          }
        });
        mergedItems.put(id, mergedMap);
      }

      list2map.remove(id);
    }

    // rest item last.
    mergedItems.putAll(list2map);

    return Lists.newArrayList(mergedItems.values());

  }

}
