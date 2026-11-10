package cz.metdat.unit;

/** Test-only bridge to the package-private CSV cell encoder. */
final class ExportServiceAccess {
    static String cell(String v) { return cz.metdat.service.ExportServiceTestBridge.cell(v); }
}
