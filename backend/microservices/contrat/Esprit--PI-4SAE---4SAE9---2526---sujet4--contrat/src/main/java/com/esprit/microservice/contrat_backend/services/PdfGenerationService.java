package com.esprit.microservice.contrat_backend.services;

import com.esprit.microservice.contrat_backend.entities.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
public class PdfGenerationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public byte[] generateContractPdf(Contract contract) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document    doc    = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(50, 50, 50, 50);

        PdfFont font     = PdfFontFactory.createFont("Helvetica");
        PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

        // ── TITRE ────────────────────────────────────────────────
        doc.add(new Paragraph("SERVICE AGREEMENT CONTRACT")
                .setFont(boldFont).setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));

        doc.add(new Paragraph("CONTRAT DE PRESTATION DE SERVICES")
                .setFont(font).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setMarginBottom(4));

        doc.add(new Paragraph("N° " + contract.getContractNumber())
                .setFont(boldFont).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        doc.add(new LineSeparator(new SolidLine()).setMarginBottom(8));

        doc.add(new Paragraph("Dated " + contract.getCreatedAt().format(FMT))
                .setFont(font).setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(14));

        // ── PARTIES ──────────────────────────────────────────────
        doc.add(new Paragraph("BETWEEN THE UNDERSIGNED:")
                .setFont(boldFont).setFontSize(11).setMarginBottom(8));

        // Tableau 2 colonnes pour les parties
        Table partiesTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        // Colonne CLIENT
        Cell clientCell = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(15);
        clientCell.add(new Paragraph("THE CLIENT,").setFont(boldFont).setFontSize(10).setMarginBottom(4));
        clientCell.add(partyLine("Full Name:", contract.getClientName(), font, boldFont));
        if (notEmpty(contract.getClientCompany()))
            clientCell.add(partyLine("Company:", contract.getClientCompany(), font, boldFont));
        clientCell.add(partyLine("Email:", contract.getClientEmail(), font, boldFont));
        if (notEmpty(contract.getClientPhone()))
            clientCell.add(partyLine("Phone:", contract.getClientPhone(), font, boldFont));

        // Colonne FREELANCER
        Cell freelancerCell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(15)
                .setBorderLeft(new SolidBorder(0.5f));
        freelancerCell.add(new Paragraph("THE SERVICE PROVIDER,").setFont(boldFont).setFontSize(10).setMarginBottom(4));
        freelancerCell.add(partyLine("Full Name:", contract.getFreelancerName(), font, boldFont));
        freelancerCell.add(partyLine("Email:", contract.getFreelancerEmail(), font, boldFont));
        if (notEmpty(contract.getFreelancerPhone()))
            freelancerCell.add(partyLine("Phone:", contract.getFreelancerPhone(), font, boldFont));
        if (notEmpty(contract.getFreelancerSpecialty()))
            freelancerCell.add(partyLine("Specialty:", contract.getFreelancerSpecialty(), font, boldFont));

        partiesTable.addCell(clientCell);
        partiesTable.addCell(freelancerCell);
        doc.add(partiesTable);

        doc.add(new LineSeparator(new SolidLine()).setMarginBottom(8));

        // ── ARTICLE 1 ────────────────────────────────────────────
        addArticleTitle(doc, "ARTICLE 1 — PURPOSE OF THE CONTRACT", boldFont);
        addInfoLine(doc, "Contract Type:", contract.getContractType().toString(), font, boldFont);
        addInfoLine(doc, "Mission Title:", contract.getMissionTitle(), font, boldFont);
        if (notEmpty(contract.getTechnologies()))
            addInfoLine(doc, "Technologies:", contract.getTechnologies(), font, boldFont);
        if (notEmpty(contract.getMissionDescription())) {
            doc.add(new Paragraph("Mission Description:").setFont(boldFont).setFontSize(9).setMarginTop(6).setMarginBottom(2));
            doc.add(new Paragraph(contract.getMissionDescription()).setFont(font).setFontSize(9).setMarginLeft(15).setMarginBottom(6));
        }
        if (notEmpty(contract.getDeliverables())) {
            doc.add(new Paragraph("Deliverables:").setFont(boldFont).setFontSize(9).setMarginTop(4).setMarginBottom(2));
            doc.add(new Paragraph(contract.getDeliverables()).setFont(font).setFontSize(9).setMarginLeft(15).setMarginBottom(6));
        }

        // ── ARTICLE 2 ────────────────────────────────────────────
        if (contract.getStartDate() != null || contract.getEndDate() != null || contract.getDurationMonths() != null) {
            addArticleTitle(doc, "ARTICLE 2 — CONTRACT DURATION", boldFont);
            if (contract.getStartDate() != null)
                addInfoLine(doc, "Start Date:", contract.getStartDate().format(FMT), font, boldFont);
            if (contract.getEndDate() != null)
                addInfoLine(doc, "End Date:", contract.getEndDate().format(FMT), font, boldFont);
            if (contract.getDurationMonths() != null)
                addInfoLine(doc, "Duration:", contract.getDurationMonths() + " months", font, boldFont);
        }

        // ── ARTICLE 3 ────────────────────────────────────────────
        addArticleTitle(doc, "ARTICLE 3 — COMPENSATION AND PAYMENT TERMS", boldFont);

        //  Locale.US pour forcer le point comme séparateur décimal
        addInfoLine(doc, "Total Amount:",
                String.format(Locale.US, "%.2f %s", contract.getTotalAmount(), contract.getCurrency()),
                font, boldFont);

        if (contract.getVatRate() != null)
            addInfoLine(doc, "VAT Rate:", contract.getVatRate() + "%", font, boldFont);

        String paymentMethod = switch (contract.getPaymentMethod().toString()) {
            case "BANK_TRANSFER" -> "Bank Transfer";
            case "CREDIT_CARD"   -> "Credit Card";
            case "PAYPAL"        -> "PayPal";
            case "CHECK"         -> "Check";
            case "CASH"          -> "Cash";
            default -> contract.getPaymentMethod().toString();
        };
        addInfoLine(doc, "Payment Method:", paymentMethod, font, boldFont);
        if (notEmpty(contract.getIban()))
            addInfoLine(doc, "IBAN:", contract.getIban(), font, boldFont);
        if (notEmpty(contract.getBic()))
            addInfoLine(doc, "BIC:", contract.getBic(), font, boldFont);

        // ── ARTICLE 4 ────────────────────────────────────────────
        addArticleTitle(doc, "ARTICLE 4 — LEGAL PROVISIONS", boldFont);
        String applicableLaw = switch (contract.getApplicableLaw().toString()) {
            case "FRENCH"   -> "French Law";
            case "TUNISIAN" -> "Tunisian Law";
            case "AMERICAN" -> "American Law";
            case "CANADIAN" -> "Canadian Law";
            case "UK"       -> "British Law";
            default -> contract.getApplicableLaw().toString();
        };
        addInfoLine(doc, "Applicable Law:", applicableLaw, font, boldFont);
        if (notEmpty(contract.getCompetentCourt()))
            addInfoLine(doc, "Competent Court:", contract.getCompetentCourt(), font, boldFont);
        addInfoLine(doc, "Confidentiality:", contract.getConfidentialityYears() + " years", font, boldFont);
        addInfoLine(doc, "IP Rights Transfer:", Boolean.TRUE.equals(contract.getIpTransferToClient()) ? "Yes" : "No", font, boldFont);
        addInfoLine(doc, "Portfolio Rights:", Boolean.TRUE.equals(contract.getPortfolioAllowed()) ? "Yes" : "No", font, boldFont);
        if (Boolean.TRUE.equals(contract.getPortfolioAllowed()) && contract.getPortfolioDelayMonths() != null)
            addInfoLine(doc, "Portfolio Delay:", contract.getPortfolioDelayMonths() + " months", font, boldFont);

        // ── SIGNATURES ───────────────────────────────────────────
        doc.add(new Paragraph("\n"));
        doc.add(new LineSeparator(new SolidLine()).setMarginBottom(12));

        doc.add(new Paragraph("SIGNATURES")
                .setFont(boldFont).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        // Tableau 2 colonnes pour les signatures côte à côte
        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // ── Signature CLIENT ─────────────────────────────────────
        Cell clientSigCell = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(20);
        clientSigCell.add(new Paragraph("The Client,").setFont(boldFont).setFontSize(10).setMarginBottom(6));
        addSignatureToCell(clientSigCell, contract.getClientSignatureImage(), font);
        // Ligne horizontale sous la signature
        clientSigCell.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(4).setMarginBottom(4));
        if (contract.getClientSignedAt() != null) {
            clientSigCell.add(new Paragraph("Signed on: " + contract.getClientSignedAt().format(FMT))
                    .setFont(font).setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            clientSigCell.add(new Paragraph("(Not signed yet)")
                    .setFont(font).setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
        }

        // ── Signature FREELANCER ─────────────────────────────────
        Cell freelancerSigCell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(20)
                .setBorderLeft(new SolidBorder(0.5f));
        freelancerSigCell.add(new Paragraph("The Service Provider,").setFont(boldFont).setFontSize(10).setMarginBottom(6));
        addSignatureToCell(freelancerSigCell, contract.getFreelancerSignatureImage(), font);
        freelancerSigCell.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(4).setMarginBottom(4));
        if (contract.getFreelancerSignedAt() != null) {
            freelancerSigCell.add(new Paragraph("Signed on: " + contract.getFreelancerSignedAt().format(FMT))
                    .setFont(font).setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY));
        } else {
            freelancerSigCell.add(new Paragraph("(Not signed yet)")
                    .setFont(font).setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
        }

        sigTable.addCell(clientSigCell);
        sigTable.addCell(freelancerSigCell);
        doc.add(sigTable);

        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Executed in two original copies, on " + contract.getCreatedAt().format(FMT))
                .setFont(font).setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));

        doc.close();
        return baos.toByteArray();
    }

    // ── Ajoute l'image de signature dans une Cell ─────────────────
    private void addSignatureToCell(Cell cell, String signatureImage, PdfFont font) {
        if (notEmpty(signatureImage)) {
            try {
                String base64 = signatureImage.contains(",")
                        ? signatureImage.substring(signatureImage.indexOf(",") + 1)
                        : signatureImage;
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                Image img = new Image(ImageDataFactory.create(imageBytes));
                img.setMaxWidth(160).setMaxHeight(65).setMarginBottom(2);
                cell.add(img);
            } catch (Exception e) {
                cell.add(new Paragraph("__________________________").setFont(font).setFontSize(10));
            }
        } else {
            cell.add(new Paragraph("__________________________").setFont(font).setFontSize(10));
        }
    }

    // ── Paragraph pour une ligne dans une Cell ────────────────────
    private Paragraph partyLine(String label, String value, PdfFont font, PdfFont boldFont) {
        Paragraph p = new Paragraph();
        p.add(new Text(label + " ").setFont(boldFont).setFontSize(9));
        p.add(new Text(value != null ? value : "—").setFont(font).setFontSize(9));
        p.setMarginBottom(2);
        return p;
    }

    private void addArticleTitle(Document doc, String title, PdfFont boldFont) {
        doc.add(new Paragraph(title)
                .setFont(boldFont).setFontSize(10)
                .setMarginTop(10).setMarginBottom(6).setUnderline());
    }

    private void addInfoLine(Document doc, String label, String value,
                             PdfFont font, PdfFont boldFont) {
        Paragraph p = new Paragraph();
        p.add(new Text(label + " ").setFont(boldFont).setFontSize(9));
        p.add(new Text(value != null ? value : "—").setFont(font).setFontSize(9));
        p.setMarginBottom(3).setMarginLeft(10);
        doc.add(p);
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}