package ${packageName};
${sourceImportStatement}
${targetImportStatement}
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE${usesClause})
public interface ${className} {
    ${targetTypeRef} toBackend(${sourceTypeRef} source);
    ${sourceTypeRef} toFrontend(${targetTypeRef} source);
}
