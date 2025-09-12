package jp.vemi.framework.crypto;

import jp.vemi.framework.crypto.algos.DefaultJavaPassphraseEncoder;
import jp.vemi.framework.util.InstanceUtil;

public class Crypto {

    private static Class<? extends HashEncoder> concreteClass = DefaultJavaPassphraseEncoder.class;

    public static HashEncoder getConcrete() {
        return getConcrete(concreteClass);
    }

    public static HashEncoder getConcrete(Class<? extends HashEncoder> concreteClass) {
        return InstanceUtil.forceCast(InstanceUtil.newInstance(concreteClass));
    }

}
