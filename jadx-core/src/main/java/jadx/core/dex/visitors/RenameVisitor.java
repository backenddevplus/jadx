package jadx.core.dex.visitors;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import jadx.api.JadxArgs;
import jadx.core.Consts;
import jadx.core.deobf.Deobfuscator;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.files.InputFile;

public class RenameVisitor extends AbstractVisitor {

	private Deobfuscator deobfuscator;

	@Override
	public void init(RootNode root) {
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return;
		}
		InputFile firstInputFile = dexNodes.get(0).getDexFile().getInputFile();
		String firstInputFileName = firstInputFile.getFile().getAbsolutePath();
		String inputPath = FilenameUtils.getFullPathNoEndSeparator(firstInputFileName);
		String inputName = FilenameUtils.getBaseName(firstInputFileName);

		File deobfMapFile = new File(inputPath, inputName + ".jobf");
		JadxArgs args = root.getArgs();
		deobfuscator = new Deobfuscator(args, dexNodes, deobfMapFile);
		boolean deobfuscationOn = args.isDeobfuscationOn();
		if (deobfuscationOn) {
			deobfuscator.execute();
		}
		checkClasses(root, args.isFsCaseSensitive());
	}

	private void checkClasses(RootNode root, boolean caseSensitive) {
		List<ClassNode> classes = root.getClasses(true);
		for (ClassNode cls : classes) {
			checkClassName(cls);
			checkFields(cls);
			checkMethods(cls);
		}
		if (!caseSensitive) {
			Set<String> clsFullPaths = new HashSet<>(classes.size());
			for (ClassNode cls : classes) {
				ClassInfo clsInfo = cls.getClassInfo();
				ClassInfo aliasClsInfo = clsInfo.getAlias();
				if (!clsFullPaths.add(aliasClsInfo.getFullPath().toLowerCase())) {
					String newShortName = deobfuscator.getClsAlias(cls);
					String newFullName = aliasClsInfo.makeFullClsName(newShortName, true);

					clsInfo.rename(root, newFullName);
					clsFullPaths.add(clsInfo.getAlias().getFullPath().toLowerCase());
				}
			}
		}
	}

	private void checkClassName(ClassNode cls) {
		ClassInfo classInfo = cls.getClassInfo();
		ClassInfo alias = classInfo.getAlias();
		String clsName = alias.getShortName();

		String newShortName = fixClsShortName(clsName);
		if (!newShortName.equals(clsName)) {
			classInfo.rename(cls.root(), alias.makeFullClsName(newShortName, true));
			alias = classInfo.getAlias();
		}
		if (alias.getPackage().isEmpty()) {
			String fullName = alias.makeFullClsName(alias.getShortName(), true);
			String newFullName = Consts.DEFAULT_PACKAGE_NAME + '.' + fullName;
			classInfo.rename(cls.root(), newFullName);
		}
	}

	private String fixClsShortName(String clsName) {
		char firstChar = clsName.charAt(0);
		if (Character.isDigit(firstChar)) {
			return Consts.ANONYMOUS_CLASS_PREFIX + NameMapper.removeInvalidCharsMiddle(clsName);
		}
		if (firstChar == '$') {
			return 'C' + NameMapper.removeInvalidCharsMiddle(clsName);
		}
		String cleanClsName = NameMapper.removeInvalidChars(clsName, "C");
		if (!NameMapper.isValidIdentifier(cleanClsName)) {
			return 'C' + cleanClsName;
		}
		return cleanClsName;
	}

	private void checkFields(ClassNode cls) {
		Set<String> names = new HashSet<>();
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			String fieldName = fieldInfo.getAlias();
			if (!names.add(fieldName) || !NameMapper.isValidIdentifier(fieldName)) {
				deobfuscator.forceRenameField(field);
			}
		}
	}

	private void checkMethods(ClassNode cls) {
		for (MethodNode mth : cls.getMethods()) {
			if (!NameMapper.isValidIdentifier(mth.getAlias())) {
				deobfuscator.forceRenameMethod(mth);
			}
		}
		Set<String> names = new HashSet<>();
		for (MethodNode mth : cls.getMethods()) {
			AccessInfo accessFlags = mth.getAccessFlags();
			if (accessFlags.isConstructor()
					|| accessFlags.isBridge()
					|| accessFlags.isSynthetic()
					|| mth.contains(AFlag.DONT_GENERATE) /* this flag not set yet */) {
				continue;
			}
			String signature = mth.getMethodInfo().makeSignature(true, false);
			if (!names.add(signature)) {
				deobfuscator.forceRenameMethod(mth);
			}
		}
	}
}
