package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfGenerationService {

    public byte[] generateContractPdf(Contract contract) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont font = PdfFontFactory.createFont("Helvetica");
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // Main title
        Paragraph title = new Paragraph("SERVICE AGREEMENT CONTRACT")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        Paragraph subTitle = new Paragraph("CONTRAT DE PRESTATION DE SERVICES")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setMarginBottom(10);
        document.add(subTitle);

        Paragraph reference = new Paragraph("N° " + contract.getContractNumber())
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(reference);

        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Paragraph date = new Paragraph("Dated " + contract.getCreatedAt().format(formatter))
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
        document.add(date);

        Paragraph preamble = new Paragraph("BETWEEN THE UNDERSIGNED:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(preamble);

        // ===== CLIENT =====
        document.add(new Paragraph("THE CLIENT,").setFont(boldFont).setFontSize(11).setMarginBottom(5));
        addInfoLine(document, "Full Name:", contract.getClientName(), font, boldFont);
        if (contract.getClientCompany() != null && !contract.getClientCompany().isEmpty()) {
            addInfoLine(document, "Company:", contract.getClientCompany(), font, boldFont);
        }
        addInfoLine(document, "Email:", contract.getClientEmail(), font, boldFont);
        if (contract.getClientPhone() != null && !contract.getClientPhone().isEmpty()) {
            addInfoLine(document, "Phone:", contract.getClientPhone(), font, boldFont);
        }
        document.add(new Paragraph("\n"));

        // ===== FREELANCER =====
        document.add(new Paragraph("THE SERVICE PROVIDER,").setFont(boldFont).setFontSize(11).setMarginBottom(5));
        addInfoLine(document, "Full Name:", contract.getFreelancerName(), font, boldFont);
        addInfoLine(document, "Email:", contract.getFreelancerEmail(), font, boldFont);
        if (contract.getFreelancerPhone() != null && !contract.getFreelancerPhone().isEmpty()) {
            addInfoLine(document, "Phone:", contract.getFreelancerPhone(), font, boldFont);
        }
        if (contract.getFreelancerSpecialty() != null && !contract.getFreelancerSpecialty().isEmpty()) {
            addInfoLine(document, "Specialty:", contract.getFreelancerSpecialty(), font, boldFont);
        }
        document.add(new Paragraph("\n"));

        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        // ===== ARTICLE 1 =====
        addArticleTitle(document, "ARTICLE 1 - PURPOSE OF THE CONTRACT", boldFont);
        addInfoLine(document, "Contract Type:", contract.getContractType().toString(), font, boldFont);
        addInfoLine(document, "Mission Title:", contract.getMissionTitle(), font, boldFont);
        if (contract.getTechnologies() != null && !contract.getTechnologies().isEmpty()) {
            addInfoLine(document, "Technologies:", contract.getTechnologies(), font, boldFont);
        }
        if (contract.getMissionDescription() != null && !contract.getMissionDescription().isEmpty()) {
            document.add(new Paragraph("Mission Description:").setFont(boldFont).setMarginTop(8).setMarginBottom(2));
            document.add(new Paragraph(contract.getMissionDescription())
                    .setFont(font).setMarginLeft(20).setMarginBottom(10));
        }
        if (contract.getDeliverables() != null && !contract.getDeliverables().isEmpty()) {
            document.add(new Paragraph("Deliverables:").setFont(boldFont).setMarginTop(5).setMarginBottom(2));
            document.add(new Paragraph(contract.getDeliverables())
                    .setFont(font).setMarginLeft(20).setMarginBottom(10));
        }

        // ===== ARTICLE 2 =====
        if (contract.getStartDate() != null || contract.getEndDate() != null || contract.getDurationMonths() != null) {
            addArticleTitle(document, "ARTICLE 2 - CONTRACT DURATION", boldFont);
            if (contract.getStartDate() != null) {
                addInfoLine(document, "Start Date:", contract.getStartDate().format(formatter), font, boldFont);
            }
            if (contract.getEndDate() != null) {
                addInfoLine(document, "End Date:", contract.getEndDate().format(formatter), font, boldFont);
            }
            if (contract.getDurationMonths() != null) {
                addInfoLine(document, "Duration:", contract.getDurationMonths() + " months", font, boldFont);
            }
        }

        // ===== ARTICLE 3 =====
        addArticleTitle(document, "ARTICLE 3 - COMPENSATION AND PAYMENT TERMS", boldFont);
        addInfoLine(document, "Total Amount:", String.format("%.2f %s", contract.getTotalAmount(), contract.getCurrency()), font, boldFont);
        if (contract.getVatRate() != null) {
            addInfoLine(document, "VAT Rate:", contract.getVatRate() + "%", font, boldFont);
        }
        String paymentMethod = switch (contract.getPaymentMethod().toString()) {
            case "BANK_TRANSFER" -> "Bank Transfer";
            case "CREDIT_CARD" -> "Credit Card";
            case "PAYPAL" -> "PayPal";
            case "CHECK" -> "Check";
            case "CASH" -> "Cash";
            default -> contract.getPaymentMethod().toString();
        };
        addInfoLine(document, "Payment Method:", paymentMethod, font, boldFont);
        if (contract.getIban() != null && !contract.getIban().isEmpty()) {
            addInfoLine(document, "IBAN:", contract.getIban(), font, boldFont);
        }
        if (contract.getBic() != null && !contract.getBic().isEmpty()) {
            addInfoLine(document, "BIC:", contract.getBic(), font, boldFont);
        }

        // ===== ARTICLE 4 =====
        addArticleTitle(document, "ARTICLE 4 - LEGAL PROVISIONS", boldFont);
        String applicableLaw = switch (contract.getApplicableLaw().toString()) {
            case "FRENCH" -> "French Law";
            case "TUNISIAN" -> "Tunisian Law";
            case "AMERICAN" -> "American Law";
            case "CANADIAN" -> "Canadian Law";
            case "UK" -> "British Law";
            default -> contract.getApplicableLaw().toString();
        };
        addInfoLine(document, "Applicable Law:", applicableLaw, font, boldFont);
        if (contract.getCompetentCourt() != null && !contract.getCompetentCourt().isEmpty()) {
            addInfoLine(document, "Competent Court:", contract.getCompetentCourt(), font, boldFont);
        }
        addInfoLine(document, "Confidentiality:", contract.getConfidentialityYears() + " years", font, boldFont);
        addInfoLine(document, "IP Rights Transfer:", contract.getIpTransferToClient() ? "Yes" : "No", font, boldFont);
        addInfoLine(document, "Portfolio Rights:", contract.getPortfolioAllowed() ? "Yes" : "No", font, boldFont);
        if (contract.getPortfolioAllowed() && contract.getPortfolioDelayMonths() != null) {
            addInfoLine(document, "Portfolio Delay:", contract.getPortfolioDelayMonths() + " months", font, boldFont);
        }

        // ===== SIGNATURES =====
        document.add(new Paragraph("\n"));
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        Paragraph signaturesTitle = new Paragraph("SIGNATURES")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(signaturesTitle);

        // Signature CLIENT - Utiliser clientSignatureImage au lieu de clientSignatureHash
        document.add(new Paragraph("The Client,").setFont(boldFont).setMarginBottom(2));
        if (contract.getClientSignatureImage() != null && !contract.getClientSignatureImage().isEmpty()) {
            try {
                document.add(addSignatureImage(contract.getClientSignatureImage()));
            } catch (Exception e) {
                System.err.println("Erreur ajout signature client: " + e.getMessage());
                document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
            }
        } else {
            document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
        }
        if (contract.getClientSignedAt() != null) {
            document.add(new Paragraph("Signed on: " + contract.getClientSignedAt().format(formatter))
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            document.add(new Paragraph("(Signature)")
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        }

        document.add(new Paragraph("\n"));

        // Signature FREELANCER - Utiliser freelancerSignatureImage
        document.add(new Paragraph("The Service Provider,").setFont(boldFont).setMarginBottom(2));
        if (contract.getFreelancerSignatureImage() != null && !contract.getFreelancerSignatureImage().isEmpty()) {
            try {
                document.add(addSignatureImage(contract.getFreelancerSignatureImage()));
            } catch (Exception e) {
                System.err.println("Erreur ajout signature freelance: " + e.getMessage());
                document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
            }
        } else {
            document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
        }
        if (contract.getFreelancerSignedAt() != null) {
            document.add(new Paragraph("Signed on: " + contract.getFreelancerSignedAt().format(formatter))
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            document.add(new Paragraph("(Signature)")
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        }

        document.add(new Paragraph("\n\n"));

        Paragraph footer = new Paragraph("Executed in two original copies")
                .setFont(font)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    // Convertit le base64 en image iText - Version CORRIGÉE
    private Image addSignatureImage(String base64Signature) {
        String base64Data = base64Signature;

        // Supprimer le préfixe data:image/png;base64, s'il existe
        if (base64Signature.contains(",")) {
            base64Data = base64Signature.split(",")[1];
        }

        // Supprimer les espaces et sauts de ligne
        base64Data = base64Data.trim().replaceAll("\\s", "");

        // Décoder le base64
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        // Créer l'image
        Image signatureImage = new Image(ImageDataFactory.create(imageBytes));
        signatureImage.setWidth(150);
        signatureImage.setHeight(60);
        signatureImage.setMarginBottom(2);

        return signatureImage;
    }

    private void addArticleTitle(Document document, String title, PdfFont boldFont) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(11)
                .setMarginTop(10)
                .setMarginBottom(8)
                .setUnderline());
    }

    private void addInfoLine(Document document, String label, String value, PdfFont font, PdfFont boldFont) {
        Paragraph p = new Paragraph();
        p.add(new Text(label + " ").setFont(boldFont));
        p.add(new Text(value != null ? value : "—").setFont(font));
        p.setMarginBottom(3);
        p.setMarginLeft(10);
        document.add(p);
    }
}