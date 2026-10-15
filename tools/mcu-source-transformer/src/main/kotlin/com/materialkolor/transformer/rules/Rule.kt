package com.materialkolor.transformer.rules

/**
 * Rule ids appear in manifests and the reviewed policy lock. Renaming one is a policy change.
 */
internal enum class Rule(
    val id: String,
) {
    AnalogousProperty("analogous-property"),
    ArraySort("array-sort"),
    Cam16Visibility("cam16-visibility"),
    CollectionImport("collection-import"),
    DiagnosticFormatPolicy("diagnostic-format-policy"),
    DynamicColorFactory("dynamic-color-factory"),
    EnumDefault("enum-default"),
    EnumLowercase("enum-lowercase"),
    HarmonizeHct("harmonize-hct"),
    HctCopyBody("hct-copy-body"),
    HctCopyMethod("hct-copy-method"),
    HctCopyType("hct-copy-type"),
    HctValueContract("hct-value-contract"),
    HidePaletteCache("hide-palette-cache"),
    ImplementationVisibility("implementation-visibility"),
    ImportPackage("import-package"),
    InitialToneDefault("initial-tone-default"),
    JvmStaticImport("jvm-static-import"),
    ListSort("list-sort"),
    LuminanceMember("luminance-member"),
    MathMax("math-max"),
    MathToDegrees("math-toDegrees"),
    MathToRadians("math-toRadians"),
    MemberVisibility("member-visibility"),
    Package("package"),
    PokoClass("poko-class"),
    PrivateListCopy("private-list-copy"),
    RemoveJvmImport("remove-jvm-import"),
    RgbFormat("rgb-format"),
    RngAlias("rng-alias"),
    RngImport("rng-import"),
    SchemeCopy("scheme-copy"),
    SchemeErrorDefault("scheme-error-default"),
    ScoreDefault("score-default"),
    ScoreNullableFallback("score-nullable-fallback"),
    ScoreOptionalFallback("score-optional-fallback"),
    SpecDefault("spec-default"),
    SubclassOptIn("subclass-opt-in"),
    SubclassOptInRequired("subclass-opt-in-required"),
    Suppression("suppression"),
    ValueEquality("value-equality"),
    ValueInputVisibility("value-input-visibility"),
    WhitePointCopy("white-point-copy"),
    ;

    override fun toString(): String = id
}
