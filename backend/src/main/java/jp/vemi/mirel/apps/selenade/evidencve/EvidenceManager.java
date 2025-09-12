/*
 * Copyright(c) 2015-2025 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.evidencve;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jp.vemi.framework.exeption.MessagingException;
import jp.vemi.mirel.apps.selenade.dto.evidence.Evidence;
import jp.vemi.mirel.apps.selenade.dto.evidence.QueryLog;

public class EvidenceManager {

    protected String header;
    private List<String> warns = Lists.newArrayList();
    private List<StackTraceElement[]> exceptions = Lists.newArrayList();
    private List<Evidence> evidences = Lists.newArrayList();
    private boolean individualHeader;

    private boolean skipDate;

    public EvidenceManager skipDate() {
        skipDate = true;
        return this;
    }

    public EvidenceManager setIndividualHeader() {
        individualHeader = true;
        return this;
    }

    private static class Const {
        public static final String LS = "\r\n";
    }

    private EvidenceManager() {
    }

    public static EvidenceManager create(String header) {
        EvidenceManager o = new EvidenceManager();
        o.header = header;
        return o;
    }

    /**
     * Evidence追加.<br/>
     *
     * @param evidence
     * @param object
     * @return
     */
    public <OBJECT> EvidenceManager append(Evidence evidence, OBJECT object) {

        // print individual header.

        // Image flie?
        if (object instanceof ImageFile) {
            ImageFile image = cast(object);
            evidence.addData(image);
            return this;
        }
        // Throwable?
        if (object instanceof Throwable) {
            x: if (object instanceof MessagingException) {

                MessagingException actual = cast(object);
                List<String> messages = actual.messages;

                if (CollectionUtils.isEmpty(messages)) {
                    break x;
                }

                for (String m : messages) {
                    appendMessage(evidence, "0000", m);
                }

                evidences.add(evidence);

                return this;
            }

            Throwable actual = cast(object);
            exceptions.add(actual.getStackTrace());

            return this;
        }

        // QueryResult?
        if (object instanceof QueryLog) {
            QueryLog qr = cast(object);

            // header
            evidence.addHeader("Query: " + qr.getSql());
            evidence.addHeader("Result count: " + qr.getCount());

            // data
            evidence.addData(qr.getResultList());

            evidences.add(evidence);
            return this;
        }

        // Collection?
        if (object instanceof List) {

            List<?> elems = cast(object);

            if (CollectionUtils.isEmpty(elems)) {
                evidence.addData("List is Empty.");
                evidences.add(evidence);
                return this;
            }

            // others
            List<Object> tmplist = cast(elems);
            evidence.addData(tmplist);

            evidences.add(evidence);

            return this;
        }

        // ?
        evidence.addData(object);
        evidences.add(evidence);

        return this;
    }

    /**
     * @param evidence
     * @param m
     */
    private void appendMessage(Evidence evidence, String messageId, String messageText) {
        evidence.addData(new _Messages(messageId, messageText));
    }

    /**
     * build.<br/>
     *
     * @return this instance.
     */
    public StringBuilder build() {

        StringBuilder printer = new StringBuilder();

        // log.
        if (false == skipDate) {
            printer.append(StringUtils.join("export date:", new Date().toString()));
            printer.append(Const.LS);
        }

        // header.
        printer.append(header);

        // warns?
        if (false == CollectionUtils.isEmpty(warns)) {
            printer.append("警告が発生しています。");
            for (String warn : warns) {
                printer.append(warn);
                printer.append(Const.LS);
            }
        }

        // exceptions?
        if (false == CollectionUtils.isEmpty(exceptions)) {
            printer.append("例外が発生しています。");
            for (StackTraceElement[] es : exceptions) {
                for (StackTraceElement e : es) {
                    printer.append(e.toString());
                }
                printer.append(Const.LS);
            }
        }

        printer.append(Const.LS);

        // evidences.
        for (Evidence evid : evidences) {
            // header
            if (individualHeader) {
                printer.append(header);
                printer.append(Const.LS);
            }

            for (Object hd : evid.getHeaders()) {
                printer.append(hd);
                printer.append(Const.LS);
            }

            // data
            List<Object> data = evid.getData();

            try {
                printModel(data, printer);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            // end
            for (Object end : evid.getFooters()) {
                printer.append(end);
                printer.append(Const.LS);
            }

            printer.append(Const.LS);
            printer.append(Const.LS);
        }

        return printer;

    }

    public void buildAndExport(String path) {
        buildAndExport(path, true);
    }

    public void buildAndExport(String path, boolean appendFlag) {
        StringBuilder printer = build();
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            pw.print(printer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buildAndPrint() {
        StringBuilder printer = build();
        System.out.println(printer.toString());

    }

    /**
     * 区切り.<br/>
     */
    protected void separate() {
        evidences.add(Evidence.$empty());
    }

    /**
     * 強制キャスト.<br/>
     *
     * @param obj
     * @return casted
     */
    @SuppressWarnings("unchecked")
    protected static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * Bizメッセージを取り扱うクラス.<br/>
     *
     * @author h_kurosawa
     *
     */
    protected static class _Messages {
        /** メッセージコード */
        public String messageCode;
        /** メッセージ詳細 */
        public String messageDetail;

        /**
         * default constructor.
         *
         * @param messageCode
         * @param messageDetail
         */
        public _Messages(String messageCode, String messageDetail) {
            this.messageCode = messageCode;
            this.messageDetail = messageDetail;
        }
    }

    /**
     * 最後に余計なTabが入っちゃうけど許してね
     *
     * @param args
     * @throws Throwable
     */
    private static <T> void printModel(List<T> args, StringBuilder output)
            throws Throwable {
    }

    public static class ImageFile extends File {

        public ImageFile(String pathname) {
            super(pathname);
        }

        public static ImageFile as(File sourceFile, String targetDir) {
            Path source = Paths.get(sourceFile.getPath());
            Path targetDirPath = Paths.get(targetDir);
            if (false == targetDirPath.toFile().exists()) {
                targetDirPath.toFile().mkdirs();
            }
            Path targetFilePath = Paths.get(targetDir + "/" + sourceFile.getName());
            try {
                Files.copy(source, targetFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new ImageFile(targetDir);
        }
    }
}
