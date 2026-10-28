package net.swordie.ms.util;

import javax.xml.stream.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static net.swordie.ms.ServerConstants.*;

public class TranslateXML {

    // Tạo một lớp nhỏ để chứa 3 chuỗi dịch
    private static class TranslatedStrings {
        String name;
        String desc;

        public TranslatedStrings(String desc) {
            this.name = null; // Bỏ qua tên, chỉ tập trung vào mô tả
            this.desc = desc;
        }

        public TranslatedStrings(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    public static void main(String[] args) {
        // ... (phần main giữ nguyên, chỉ thay đổi các hàm bên dưới) ...
        String[] names = new String[]{"Eqp", "Use", "Ins", "Etc", "Cash"};
        for (String name : names) {
            final String INPUT_XML_PATH = WZ_DIR + "/String.wz/" + name + ".img.xml";
            final String TRANSLATED_TXT_PATH = RESOURCES_DIR + "/string/" + name + "_vn.txt";
            final String OUTPUT_XML_PATH = WZ_DIR + "/String.wz/" + name + "_vn.img.xml";

            TranslateXML processor = new TranslateXML();
            try {
                System.out.println("正在從以下路徑加載翻譯數據：" + TRANSLATED_TXT_PATH);
                Map<String, TranslatedStrings> translatedData = processor.loadTranslationMap(TRANSLATED_TXT_PATH);
                System.out.println("成功加載 " + translatedData.size() + " 個字符串。");
                System.out.println("正在替換 XML 中的字符串：" + INPUT_XML_PATH);
                processor.importAndReplace(INPUT_XML_PATH, translatedData, OUTPUT_XML_PATH);
                System.out.println("處理完成。翻譯後的 XML 文件爲：" + OUTPUT_XML_PATH);
            } catch (FileNotFoundException e) {
                System.err.println("\n[嚴重錯誤]: 未找到翻譯 TXT 或 XML 文件。");
                System.err.println("檢查路徑：" + e.getMessage());
            }
        }
    }

    // Cập nhật: Tải cả 3 chuỗi (Name, Desc, Prop)
    private Map<String, TranslatedStrings> loadTranslationMap(String translatedTxtPath) throws FileNotFoundException {
        // Vẫn sử dụng Map<String, TranslatedStrings> để tương thích với hàm importAndReplace
        Map<String, TranslatedStrings> translationMap = new HashMap<>();

        // Helper để loại bỏ dấu nháy kép bọc ngoài và escape nháy kép bên trong
        java.util.function.Function<String, String> cleanString = s ->
                s.trim().replaceAll("^\"|\"$", "").replace("\"\"", "\"");

        try (Scanner scanner = new Scanner(new File(translatedTxtPath), "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // Tách thành 3 phần: ID, Name, và Description
                // Ta cần 3 phần tử, nên giới hạn tách là 3
                // Regex phức tạp để xử lý đúng các trường CSV/chuỗi bọc trong dấu nháy kép.
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 3);

                if (parts.length < 3) {
                    System.err.println("跳過無效行（缺少 Name 或 Description）：" + line);
                    continue;
                }

                String id = parts[0].trim();
                String desc = cleanString.apply(parts[1]);
                //String name = cleanString.apply(parts[1]);
                //String desc = cleanString.apply(parts[2]);

                translationMap.put(id, new TranslatedStrings(desc));
                //translationMap.put(id, new TranslatedStrings(name, desc));
            }
        }
        return translationMap;
    }

    public void importAndReplace(String inputXmlPath, Map<String, TranslatedStrings> translationMap, String outputXmlPath) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        String currentImgdirId = null; // ID imgdir hiện tại

        try (
                FileInputStream fis = new FileInputStream(inputXmlPath);
                FileOutputStream fos = new FileOutputStream(outputXmlPath)
        ) {
            XMLStreamReader reader = inputFactory.createXMLStreamReader(fis);
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(fos, "UTF-8");

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    // Ghi lại khai báo XML
                    case XMLStreamConstants.START_DOCUMENT:
                        writer.writeStartDocument(reader.getEncoding(), reader.getVersion());
                        break;

                    case XMLStreamConstants.START_ELEMENT:
                        String tagName = reader.getLocalName();

                        // *** KHẮC PHỤC LỖI NAMESPACE URI ***
                        // Bắt đầu thẻ (chỉ dùng local name)
                        writer.writeStartElement(tagName);

                        // Ghi lại Namespaces trước các thuộc tính
                        for (int i = 0; i < reader.getNamespaceCount(); i++) {
                            writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
                        }
                        // **********************************

                        // 1. Cập nhật ID imgdir hiện tại
                        if ("imgdir".equals(tagName)) {
                            String imgdirName = reader.getAttributeValue(null, "name");
                            // Chỉ lấy ID nếu nó là số (vd: 12000)
                            if (imgdirName != null && imgdirName.matches("\\d+")) {
                                currentImgdirId = imgdirName;
                            } else {
                                currentImgdirId = null; // Bỏ qua các imgdir không phải ID (vd: "Skin")
                            }
                        }
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attrName = reader.getAttributeLocalName(i);
                            String attrValue = reader.getAttributeValue(i);
                            String translatedValue = attrValue;
                            /*if ("string".equals(tagName) && currentImgdirId != null) {
                                String stringName = reader.getAttributeValue(null, "name");
                                if ("value".equals(attrName)) {
                                    if ("name".equals(stringName)) {
                                        TranslatedStrings translated = translationMap.get(currentImgdirId);
                                        if (translated != null) {
                                            translatedValue = translated.name;
                                        }
                                    } else if ("desc".equals(stringName)) {
                                        TranslatedStrings translated = translationMap.get(currentImgdirId);
                                        if (translated != null) {
                                            translatedValue = translated.desc;
                                        }
                                    }
                                }
                            }*/
                            if ("string".equals(tagName) && currentImgdirId != null) {
                                String stringName = reader.getAttributeValue(null, "name");
                                if ("value".equals(attrName) && "desc".equals(stringName)) {
                                    TranslatedStrings translated = translationMap.get(currentImgdirId);
                                    if (translated != null) {
                                        translatedValue = translated.desc;
                                    }
                                }
                            }


                            String attrPrefix = reader.getAttributePrefix(i);
                            String attrNamespace = reader.getAttributeNamespace(i);

                            if (attrPrefix != null && attrNamespace != null && !attrNamespace.isEmpty()) {
                                writer.writeAttribute(attrPrefix, attrNamespace, attrName, translatedValue);
                            }
                            else {
                                writer.writeAttribute(attrName, translatedValue);
                            }
                        }
                        break;

                    // SAO CHÉP Y NGUYÊN các thành phần khác để giữ cấu trúc XML
                    case XMLStreamConstants.END_ELEMENT:
                        writer.writeEndElement();
                        break;
                    case XMLStreamConstants.END_DOCUMENT:
                        writer.writeEndDocument();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.SPACE:
                        writer.writeCharacters(reader.getText());
                        break;
                    // Xử lý các event còn lại
                    case XMLStreamConstants.COMMENT:
                        writer.writeComment(reader.getText());
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
                        break;
                    case XMLStreamConstants.DTD:
                    case XMLStreamConstants.ENTITY_REFERENCE:
                        break;
                }
            }

            writer.flush();
            System.out.println("替換完成。翻譯後的 XML 文件保存到：" + outputXmlPath);

        } catch (XMLStreamException | IOException e) {
            System.err.println("Import 和 Replace 過程中出錯。");
            e.printStackTrace();
        }
    }
}