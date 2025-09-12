package jp.vemi.ste.domain.context;

/**
 * Defaultのコンテキストです。<br/>
 * @author mirelplatform
 *
 */
public class DefaultSteContext extends SteContext {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7692434271295121872L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStencilCanonicalName() {
        return getStirngContent(super.get("stencilCanonicalName"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFeatureName() {
        return getStirngContent(super.get("featureName"));
    }

    protected static String getStirngContent(Object object) {

        if (object instanceof StringContent) {
            return ((StringContent) object).content;
        }
        if (object instanceof String) {
            return (String) object;
        }
        return "";
    }

    @Override
    public String getSerialNo() {
        return getStirngContent(super.get("serialNo"));
    }
}
