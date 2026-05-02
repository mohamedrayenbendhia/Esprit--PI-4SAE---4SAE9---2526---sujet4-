declare module 'jspdf-autotable' {
  import { jsPDF } from 'jspdf';

  interface AutoTableOptions {
    head?: any[][];
    body?: any[][];
    startY?: number;
    theme?: string;
    styles?: any;
    headStyles?: any;
    bodyStyles?: any;
    columnStyles?: Record<number, any>;
    margin?: any;
    tableWidth?: string | number;
    [key: string]: any;
  }

  function autoTable(doc: jsPDF, options: AutoTableOptions): void;
  export default autoTable;
}
