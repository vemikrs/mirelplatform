/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.agent;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codeborne.selenide.SelenideConfig;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.compress.utils.Lists;
import org.springframework.util.CollectionUtils;

import jp.vemi.framework.exeption.MirelApplicationException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Selenade の Suite を管理するオブジェクトです。
 */
@Getter
@Setter
public class SelenideSuite {

    final SelenideConfig config;
    final Map<String, Scenario> scenarios = Maps.newLinkedHashMap();

    public SelenideSuite(SelenideConfig config) {
        this.config = config;
    }

    /**
     * 抽象.<br/>
     */
    protected abstract static class AbstractInSuite {
        Map<String, Object> bind;
        boolean screenShot;
        public void screenShot() {
            this.screenShot = true;
        }
        public boolean isScreenshot() {
            return false;
        }
        public boolean validate() {
            return true;
        }
    }

    /**
     * シナリオ.<br/>
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Scenario extends AbstractInSuite {
        long sort;
        List<Usecase> usecases = Lists.newArrayList();
    }

    /**
     * ユースケース.<br/>
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Usecase extends AbstractInSuite {
        long sort;
        List<Action> actions = Lists.newArrayList();
    }

    /**
     * アクション.<br/>
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Action extends AbstractInSuite {
        /**
         * Sort no.
         * <p>
         * Necessary to sort yourself.
         * </p>
         */
        long sort;

        /**
         * action type.
         */
        ActionType actionType;

        /**
         * parameter.
         * <p>
         * key/value of parameter.
         * </p>
         */
        Map<String, Object> actionParameter;

        /**
         * Enumeration of action type.
         */
        public enum ActionType {
            /** 開く */
            OPEN("open"),
            /** タイトル */
            TITLE("title"),
            /** FIND */
            FIND("find"),
            /** FINDALL */
            FINDALL("findAll"),
            /** クリック */
            CLICK("click"),
            /**  */
            GET_SELECTED_RADIO("getSelectedRadio"),
            /** QUERY */
            QUERY("query"),
            ;

            /**
             * Parameter define.
             */
            static Map<ActionType, Map<Long, Set<String>>> PARAM_DEF;
            static {
                PARAM_DEF = Maps.newLinkedHashMap();
                PARAM_DEF.put(OPEN, newParameterDefine(1, "url"));
                PARAM_DEF.put(TITLE, newParameterDefine(1));
                PARAM_DEF.put(FIND, newParameterDefine(1, "pageObjectAccessor"));
                PARAM_DEF.put(FINDALL, newParameterDefine(1, "pageObjectAccessor"));
                PARAM_DEF.put(CLICK, newParameterDefine(1, "pageObjectAccessor"));
                PARAM_DEF.put(GET_SELECTED_RADIO, newParameterDefine(0));
                PARAM_DEF.put(QUERY, newParameterDefine(0));
            }

            /**
             * Name of action.
             */
            String name;

            /**
             * Default constructor.
             */
            ActionType(String name) {
                this.name = name;
            }

            public Set<String> getParameterDefine(ActionType actionType, long numberOfParameters) {
                Map<Long, Set<String>> map = PARAM_DEF.get(actionType);
                if (CollectionUtils.isEmpty(map)) {
                    throw new MirelApplicationException();
                    // TODO msg: incorrect action.
                }
                Set<String> set = map.get(numberOfParameters);
                if (CollectionUtils.isEmpty(set)) {
                    throw new MirelApplicationException();
                    // TODO msg incorrect number of parameters.
                }
                return set;
            }

            /* utility */

            /**
             * newParameterDefine
             */
            private static Map<Long, Set<String>> newParameterDefine(long numberOfParameters, String... parameters) {
                if (numberOfParameters != parameters.length) {
                    throw new IllegalArgumentException("expect parameter count is " + numberOfParameters + ", but " + parameters.length);
                }
                Map<Long, Set<String>> map = Maps.newLinkedHashMap();
                Set<String> set = Sets.newLinkedHashSet();
                if (0 < parameters.length) {
                    set.addAll(Arrays.asList(parameters));
                }
                map.put(numberOfParameters, set);
                return map;
            }
        
        }
    }

    public void sort() {
        // TODO impl.
        return;
    }

    public static List<String> validate(Action action) {
        List<String> messages = Lists.newArrayList();

        if (null == action.getActionType()) {
            messages.add("actionType is undefined.");
        }

        // valid arguments.

        // no args.
        // if (action.getActionType() && false == CollectionUtils.isEmpty(action.getActionParameter())) {
        //     messages.add("actionType is " + action.getActionType() + " but argument is  declared.");
        // }
        
        return messages;
    }

}
