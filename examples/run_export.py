#!/usr/bin/env python3
"""
CLI Wrapper for headless execution of the Groovy Annotation Exporter
script for QuPath

This script DOESN'T implement the export logic itself: its purpose is to build and run
the QuPath CLI command corresponding to:

    "$QUPATH" script --project "$PROJECT" \\
        --args "[differentiateChildren,filterMode,class1,class2,...]" \\
        "$SCRIPT"

QuPath, once launched with --project, automatically runs the script
once for each image in the project (same behavior as in "Run for Project")

Usage examples
------------
Default export, no filtering:

    python3 run_export.py \\
        --qupath /opt/QuPath/bin/QuPath \\
        --project /data/progetto/project.qpproj \\
        --script /data/scripts/export_annotations.groovy

Excluding some classes, no child differentiation:

    python3 run_export.py \\
        --qupath /opt/QuPath/bin/QuPath \\
        --project /data/progetto/project.qpproj \\
        --script /data/scripts/export_annotations.groovy \\
        --no-child-differentiation \\
        --filter-mode EXCLUDE \\
        --classes artefatto fondo

Print of the command that would be executed, doesn't run it:

    python3 run_export.py --qupath ... --project ... --script ... --dry-run
"""

from __future__ import annotations

import argparse
import shlex
import subprocess
import sys
from pathlib import Path


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="run_export.py",
        description=(
            "Runs the Groovy Annotation Exporter script in headless mode "
            "on each image of a QuPath project,\ndelegating the iteration "
            "over the images to QuPath itself "
            "(same as using 'Run for Project' int the GUI)."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=(
            "Note:\n"
            "The path to the QuPath executable can be set only once "
            "by the environment variable QUPATH_EXECUTABLE,\nto avoid "
            "having to repeat it each time the script is executed"
        ),
    )

    parser.add_argument(
        "--qupath",
        type=Path,
        help=(
            "Path to the QuPath executable. If not set, it's read from the "
            "environment variable QUPATH_EXECUTABLE."
        ),
    )
    parser.add_argument(
        "--project",
        type=Path,
        required=True,
        help="Path to the QuPath project's .qpproj file.",
    )
    parser.add_argument(
        "--script",
        type=Path,
        required=True,
        help="Path to the .groovy script to run.",
    )

    nuclei_group = parser.add_mutually_exclusive_group()
    nuclei_group.add_argument(
        "--differentiate-children",
        dest="differentiate_children",
        action="store_true",
        default=True,
        help=(
            "Gives children a different label than their parents\n"
            "(default behavior, see the main README for information on what children and parent mean)."
        ),
    )
    nuclei_group.add_argument(
        "--no-differentiate-children",
        dest="differentiate_children",
        action="store_false",
        help="Includes the children area in the parent's, without differentiating labels.",
    )

    parser.add_argument(
        "--filter-mode",
        choices=["NONE", "EXCLUDE", "INCLUDE"],
        default="NONE",
        help=(
            "Filter mode for annotation classes. "
            "NONE = no filter (default), "
            "EXCLUDE = ignores the classes listed in --classes, "
            "INCLUDE = keeps ONLY the classes listed in --classes."
        ),
    )
    parser.add_argument(
        "--classes",
        type=str,
        default="",
        metavar="CLASSES",
        help=(
            "The names of the classes (case-insensitive) to include or exclude, "
            "depending on --filter-mode. Names must be COMMA SEPARATED and enclosed in quotation marks. "
            "Ignored if --filter-mode is NONE. "
            "E.g: --classes \"squamous epithelial cells,artifact,noise\""
        ),
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Prints the command that would be run, without actually running it.",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Doesn't print the command before running it.",
    )

    return parser


def resolve_qupath_executable(args: argparse.Namespace) -> Path:
    """Resolves the path of QuPath's executable from --qupath or from QUPATH_EXECUTABLE."""
    import os

    if args.qupath is not None:
        return args.qupath

    env_value = os.environ.get("QUPATH_EXECUTABLE")
    if env_value:
        return Path(env_value)

    print(
        "Error: you must specify QuPath's path using --qupath or "
        "setting the environment variable QUPATH_EXECUTABLE.",
        file=sys.stderr,
    )
    sys.exit(2)


def parse_class_names(raw: str) -> list[str]:
    """
    Converts the comma separated string in --classes in a list of strings,
    trims trailing spaces for each element while preserving separating spaces
    Empty strings (es. double commas o spaces) are discarded.
    """
    if not raw:
        return []
    return [name.strip() for name in raw.split(",") if name.strip()]


def validate_paths(project: Path, script: Path) -> None:
    errors = []
    if not project.exists():
        errors.append(f"Project not found: {project}")
    if not script.exists():
        errors.append(f"Script not found: {script}")

    if errors:
        for e in errors:
            print(f"Error: {e}", file=sys.stderr)
        sys.exit(2)


def build_args_string(differentiate_children: bool, filter_mode: str, classes: list[str]) -> str:
    """
    Builds the string in the format "[value1,value2,...]" expected
    by the argument --args in QuPath.
    """
    values = [str(differentiate_children).lower(), filter_mode, *classes]
    return "[" + ",".join(values) + "]"


def build_command(args: argparse.Namespace, qupath_executable: Path) -> list[str]:
    classes = parse_class_names(args.classes)
    args_string = build_args_string(args.differentiate_children, args.filter_mode, classes)

    return [
        str(qupath_executable),
        "script",
        "--project", str(args.project),
        "--args", args_string,
        str(args.script),
    ]


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    qupath_executable = resolve_qupath_executable(args)
    validate_paths(args.project, args.script)

    command = build_command(args, qupath_executable)

    if not args.quiet or args.dry_run:
        print("Comando:", shlex.join(command))

    if args.dry_run:
        return 0

    result = subprocess.run(command)
    return result.returncode


if __name__ == "__main__":
    sys.exit(main())