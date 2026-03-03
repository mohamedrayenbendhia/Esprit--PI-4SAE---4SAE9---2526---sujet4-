package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.*;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    public byte[] generateContractPdf(Contract contract) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Polices
        PdfFont font = PdfFontFactory.createFont("Helvetica");
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // Titre principal
        Paragraph title = new Paragraph("CONTRAT DE PRESTATION DE SERVICES")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        Paragraph subTitle = new Paragraph("SERVICE AGREEMENT")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setMarginBottom(10);
        document.add(subTitle);

        // Référence du contrat
        Paragraph reference = new Paragraph("N° " + contract.getContractNumber())
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(reference);

        // Ligne de séparation
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        // Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Paragraph date = new Paragraph("Fait le " + contract.getCreatedAt().format(formatter))
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
        document.add(date);

        // Préambule
        Paragraph preamble = new Paragraph("ENTRE LES SOUSSIGNÉS :")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(preamble);

        // ===== CLIENT =====
        document.add(new Paragraph("LE CLIENT,").setFont(boldFont).setFontSize(11).setMarginBottom(5));
        addInfoLine(document, "Nom complet :", contract.getClientName(), font, boldFont);
        if (contract.getClientCompany() != null && !contract.getClientCompany().isEmpty()) {
            addInfoLine(document, "Société :", contract.getClientCompany(), font, boldFont);
        }
        addInfoLine(document, "Email :", contract.getClientEmail(), font, boldFont);
        if (contract.getClientPhone() != null && !contract.getClientPhone().isEmpty()) {
            addInfoLine(document, "Téléphone :", contract.getClientPhone(), font, boldFont);
        }
        document.add(new Paragraph("\n"));

        // ===== FREELANCER =====
        document.add(new Paragraph("LE PRESTATAIRE,").setFont(boldFont).setFontSize(11).setMarginBottom(5));
        addInfoLine(document, "Nom complet :", contract.getFreelancerName(), font, boldFont);
        addInfoLine(document, "Email :", contract.getFreelancerEmail(), font, boldFont);
        if (contract.getFreelancerPhone() != null && !contract.getFreelancerPhone().isEmpty()) {
            addInfoLine(document, "Téléphone :", contract.getFreelancerPhone(), font, boldFont);
        }
        if (contract.getFreelancerSpecialty() != null && !contract.getFreelancerSpecialty().isEmpty()) {
            addInfoLine(document, "Spécialité :", contract.getFreelancerSpecialty(), font, boldFont);
        }
        document.add(new Paragraph("\n"));

        // Ligne de séparation
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        // ===== ARTICLE 1: OBJET DU CONTRAT =====
        addArticleTitle(document, "ARTICLE 1 - OBJET DU CONTRAT", boldFont);

        addInfoLine(document, "Type de contrat :", contract.getContractType().toString(), font, boldFont);
        addInfoLine(document, "Intitulé de la mission :", contract.getMissionTitle(), font, boldFont);

        if (contract.getTechnologies() != null && !contract.getTechnologies().isEmpty()) {
            addInfoLine(document, "Technologies :", contract.getTechnologies(), font, boldFont);
        }

        if (contract.getMissionDescription() != null && !contract.getMissionDescription().isEmpty()) {
            document.add(new Paragraph("Description de la mission :").setFont(boldFont).setMarginTop(8).setMarginBottom(2));
            document.add(new Paragraph(contract.getMissionDescription())
                    .setFont(font)
                    .setMarginLeft(20)
                    .setMarginBottom(10));
        }

        if (contract.getDeliverables() != null && !contract.getDeliverables().isEmpty()) {
            document.add(new Paragraph("Livrables :").setFont(boldFont).setMarginTop(5).setMarginBottom(2));
            document.add(new Paragraph(contract.getDeliverables())
                    .setFont(font)
                    .setMarginLeft(20)
                    .setMarginBottom(10));
        }

        // ===== ARTICLE 2: DURÉE =====
        if (contract.getStartDate() != null || contract.getEndDate() != null || contract.getDurationMonths() != null) {
            addArticleTitle(document, "ARTICLE 2 - DURÉE DU CONTRAT", boldFont);

            if (contract.getStartDate() != null) {
                addInfoLine(document, "Date de début :", contract.getStartDate().format(formatter), font, boldFont);
            }
            if (contract.getEndDate() != null) {
                addInfoLine(document, "Date de fin :", contract.getEndDate().format(formatter), font, boldFont);
            }
            if (contract.getDurationMonths() != null) {
                addInfoLine(document, "Durée :", contract.getDurationMonths() + " mois", font, boldFont);
            }
        }

        // ===== ARTICLE 3: RÉMUNÉRATION =====
        addArticleTitle(document, "ARTICLE 3 - RÉMUNÉRATION ET MODALITÉS DE PAIEMENT", boldFont);

        addInfoLine(document, "Montant total :", String.format("%.2f %s", contract.getTotalAmount(), contract.getCurrency()), font, boldFont);

        if (contract.getVatRate() != null) {
            addInfoLine(document, "TVA :", contract.getVatRate() + "%", font, boldFont);
        }

        String paymentMethod = switch (contract.getPaymentMethod().toString()) {
            case "BANK_TRANSFER" -> "Virement bancaire";
            case "CREDIT_CARD" -> "Carte bancaire";
            case "PAYPAL" -> "PayPal";
            case "CHECK" -> "Chèque";
            case "CASH" -> "Espèces";
            default -> contract.getPaymentMethod().toString();
        };
        addInfoLine(document, "Mode de paiement :", paymentMethod, font, boldFont);

        if (contract.getIban() != null && !contract.getIban().isEmpty()) {
            addInfoLine(document, "IBAN :", contract.getIban(), font, boldFont);
        }
        if (contract.getBic() != null && !contract.getBic().isEmpty()) {
            addInfoLine(document, "BIC :", contract.getBic(), font, boldFont);
        }

        // ===== ARTICLE 4: DISPOSITIONS LÉGALES =====
        addArticleTitle(document, "ARTICLE 4 - DISPOSITIONS LÉGALES", boldFont);

        String applicableLaw = switch (contract.getApplicableLaw().toString()) {
            case "FRENCH" -> "Droit français";
            case "TUNISIAN" -> "Droit tunisien";
            case "AMERICAN" -> "Droit américain";
            case "CANADIAN" -> "Droit canadien";
            case "UK" -> "Droit britannique";
            default -> contract.getApplicableLaw().toString();
        };
        addInfoLine(document, "Droit applicable :", applicableLaw, font, boldFont);

        if (contract.getCompetentCourt() != null && !contract.getCompetentCourt().isEmpty()) {
            addInfoLine(document, "Tribunal compétent :", contract.getCompetentCourt(), font, boldFont);
        }

        addInfoLine(document, "Confidentialité :", contract.getConfidentialityYears() + " ans", font, boldFont);
        addInfoLine(document, "Cession des droits IP :", contract.getIpTransferToClient() ? "Oui" : "Non", font, boldFont);
        addInfoLine(document, "Droit de présentation :", contract.getPortfolioAllowed() ? "Oui" : "Non", font, boldFont);

        if (contract.getPortfolioAllowed() && contract.getPortfolioDelayMonths() != null) {
            addInfoLine(document, "Délai avant présentation :", contract.getPortfolioDelayMonths() + " mois", font, boldFont);
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

        // Signature client
        document.add(new Paragraph("Le Client,").setFont(boldFont).setMarginBottom(2));
        document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
        if (contract.getClientSignedAt() != null) {
            document.add(new Paragraph("Signé le : " + contract.getClientSignedAt().format(formatter))
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            document.add(new Paragraph("(Signature)").setFont(font).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        }

        document.add(new Paragraph("\n"));

        // Signature freelancer
        document.add(new Paragraph("Le Prestataire,").setFont(boldFont).setMarginBottom(2));
        document.add(new Paragraph("__________________________").setFont(font).setMarginBottom(2));
        if (contract.getFreelancerSignedAt() != null) {
            document.add(new Paragraph("Signé le : " + contract.getFreelancerSignedAt().format(formatter))
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            document.add(new Paragraph("(Signature)").setFont(font).setFontSize(9).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        }

        document.add(new Paragraph("\n\n"));

        // Footer
        Paragraph footer = new Paragraph("Fait en deux exemplaires originaux")
                .setFont(font)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY);
        document.add(footer);

        document.close();
        return baos.toByteArray();
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