package com.airline.user.service;

import com.airline.user.entity.Booking;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Service for generating E-Ticket PDFs.
 * Uses OpenPDF library for PDF generation.
 */
@Service
public class TicketService {

    /**
     * Generate E-Ticket PDF for a confirmed booking.
     * 
     * @param booking       The confirmed booking
     * @param passengerName Name of the passenger
     * @return PDF as byte array
     */
    public byte[] generateTicketPdf(Booking booking, String passengerName) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // === HEADER ===
            addHeader(document);

            // === TICKET TITLE ===
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(0, 102, 204));
            Paragraph title = new Paragraph("✈ E-TICKET", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // === PNR / BOOKING REFERENCE ===
            String pnr = (booking.getPnr() != null) ? booking.getPnr()
                    : ("PNR" + String.format("%06d", booking.getId()));
            Font pnrFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(220, 53, 69));
            Paragraph pnrPara = new Paragraph("Booking Reference: " + pnr, pnrFont);
            pnrPara.setAlignment(Element.ALIGN_CENTER);
            pnrPara.setSpacingAfter(30);
            document.add(pnrPara);

            // === BOOKING DETAILS TABLE ===
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(90);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            // Table styling
            float[] columnWidths = { 1f, 2f };
            table.setWidths(columnWidths);

            // Add rows
            addTableRow(table, "Passenger Name", passengerName != null ? passengerName : "Guest");
            addTableRow(table, "Flight Number", booking.getFlightNumber());
            addTableRow(table, "Route", booking.getSource() + " → " + booking.getDestination());
            addTableRow(table, "Date of Journey",
                    booking.getDateOfJourney() != null ? booking.getDateOfJourney() : "N/A");
            addTableRow(table, "Seat Number", booking.getSeatNumber() != null ? booking.getSeatNumber() : "N/A");
            addTableRow(table, "Class", booking.getSeatClass() != null ? booking.getSeatClass() : "Economy");
            addTableRow(table, "Price", "$" + (booking.getPrice() != null ? booking.getPrice() : "0.00"));
            addTableRow(table, "Status", booking.getBookingStatus().name());

            document.add(table);

            // === CONFIRMATION BOX ===
            PdfPTable confirmBox = new PdfPTable(1);
            confirmBox.setWidthPercentage(90);

            PdfPCell confirmCell = new PdfPCell();
            confirmCell.setBackgroundColor(new Color(212, 237, 218));
            confirmCell.setBorderColor(new Color(40, 167, 69));
            confirmCell.setPadding(15);

            Font confirmFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(21, 87, 36));
            Paragraph confirmText = new Paragraph("✓ BOOKING CONFIRMED", confirmFont);
            confirmText.setAlignment(Element.ALIGN_CENTER);
            confirmCell.addElement(confirmText);

            confirmBox.addCell(confirmCell);
            document.add(confirmBox);

            // === FOOTER ===
            addFooter(document);

        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    /**
     * Add header section to the PDF.
     */
    private void addHeader(Document document) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
        Paragraph header = new Paragraph("AIRLINE TICKET BOOKING SYSTEM", headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(10);
        document.add(header);

        // Separator line using simple paragraph
        Paragraph line = new Paragraph("─────────────────────────────────────────────────────");
        line.setAlignment(Element.ALIGN_CENTER);
        document.add(line);
        document.add(Chunk.NEWLINE);
    }

    /**
     * Add footer section to the PDF.
     */
    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        Font footerFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph(
                "This is an electronic ticket. Please present this document at the airport check-in counter.\n" +
                        "Thank you for choosing our airline. Have a safe journey!",
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        // Terms
        document.add(Chunk.NEWLINE);
        Font termsFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
        Paragraph terms = new Paragraph(
                "Terms & Conditions apply. This ticket is non-transferable. " +
                        "Please arrive at least 2 hours before departure.",
                termsFont);
        terms.setAlignment(Element.ALIGN_CENTER);
        document.add(terms);
    }

    /**
     * Add a row to the details table.
     */
    private void addTableRow(PdfPTable table, String label, String value) {
        // Label cell
        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(73, 80, 87));
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderColor(new Color(222, 226, 230));
        labelCell.setPadding(10);
        labelCell.setBackgroundColor(new Color(248, 249, 250));
        table.addCell(labelCell);

        // Value cell
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorderColor(new Color(222, 226, 230));
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }
}
