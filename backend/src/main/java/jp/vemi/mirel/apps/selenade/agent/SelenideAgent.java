/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;

import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.mirel.apps.selenade.agent.SelenideSuite.Action.ActionType;

/**
 * Selenide の Agent です。
 */
public class SelenideAgent {

    /**
     * invoke.<br/>
     * 
     * @param suite {@link SelenideSuite テストスイートの定義}
     */
    public void invoke(SelenideSuite suite) {

        suite.sort();

        SelenideConfig config = suite.config != null ? suite.config : new SelenideConfig();
        SelenideDriver driver = new SelenideDriver(config);

        for (Map.Entry<String, SelenideSuite.Scenario> scenarioEntry : suite.getScenarios().entrySet()) {
            SelenideSuite.Scenario scenario = scenarioEntry.getValue(); 
            for (SelenideSuite.Usecase usecase : scenario.usecases) {
                for (SelenideSuite.Action action : usecase.actions) {
                    action(action.actionType, action.getActionParameter());
                }
            }
        }
        driver.screenshot("file.jpg");
        driver.close();
    }

    protected void action(SelenideSuite.Action.ActionType actionType, Map<String, Object> parameter) {

        // common validate.
        isValidArgsCount(actionType, parameter);

        switch(actionType) {
            case OPEN:
                open(parameter);
                return;
            case FIND:
                return;
            case FINDALL:
                return;
            case CLICK:
                return;
            case GET_SELECTED_RADIO:
                return;

            // tool
            case QUERY:
                return;

            // assert
            case TITLE:
                return;
            default:
            throw newApplicationException("利用可能な Action でありません。" + actionType.name());
        }
    }

    private void isValidArgsCount(ActionType actionType, Map<String, Object> parameter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isValidArgsCount'");
    }

    protected void open(Map<String, Object> parameter) {
        // TODO implementation
    }

    protected boolean isValidString(Object object) {
        return object instanceof String;
    }

    protected String asString(Object object) {
        return (String) object;
    }

    /**
     * main.<br/>
     * <p>
     * API for direct access.
     * </p>
     * @param args
     */
    public static void main(String[] args) {
        SelenideAgent me = new SelenideAgent();
        me.invoke(createDefaultSuite());
    }

    protected static SelenideSuite createDefaultSuite() {
        SelenideConfig config = new SelenideConfig();
        SelenideSuite suite = new SelenideSuite(config);
        suite.scenarios.put("", new SelenideSuite.Scenario());
        return suite;
    }

    /**
     * New application exception.<br/>
     * 
     * @param messageArray
     * @return {@link MirelApplicationException}
     */
    protected MirelApplicationException newApplicationException(String... messageArray) {
        List<String> messages = Arrays.asList(messageArray);
        return new MirelApplicationException(messages);
    }
}
